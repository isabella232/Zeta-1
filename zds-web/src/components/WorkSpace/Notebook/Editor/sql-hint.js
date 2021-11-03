// CodeMirror, copyright (c) by Marijn Haverbeke and others
// Distributed under an MIT license: https://codemirror.net/LICENSE
import { getColumnJsonByTableName } from '@/components/WorkSpace/Notebook/Editor/autoCompleteJson';
(function (mod) {
  if (typeof exports == 'object' && typeof module == 'object') // CommonJS
    mod(require('codemirror'), require('codemirror/mode/sql/sql'));
  else if (typeof define == 'function' && define.amd) // AMD
    define(['../../../../../node_modules/codemirror/lib/codemirror', '../../../../../node_modules/codemirror/mode/sql/sql'], mod);
  else // Plain browser env
    mod(CodeMirror);
})(function (CodeMirror) {
  'use strict';

  let tables;
  let columns;
  //var defaultTable;
  let keywords;
  let identifierQuote;
  const CONS = {
    QUERY_DIV: ';',
    ALIAS_KEYWORD: 'AS',
    LEFT_PARENTTHESIS: '(',
  };
  const tablePreKeywords = ['TABLE', 'TRUNCATE', 'UPDATE', 'JOIN', 'FROM', 'INTO'];
  const columnPreKeywords = ['COLUMN', 'SELECT', 'WHERE', 'SET', 'ON', 'GROUP', 'ORDER', 'ADD', 'DROP'];
  const Pos = CodeMirror.Pos, cmpPos = CodeMirror.cmpPos;

  function isArray (val) { return Object.prototype.toString.call(val) == '[object Array]'; }

  function getKeywords (editor) {
    let mode = editor.doc.modeOption;
    if (mode === 'sql') mode = 'text/x-sql';
    return CodeMirror.resolveMode(mode).keywords;
  }

  function getIdentifierQuote (editor) {
    let mode = editor.doc.modeOption;
    if (mode === 'sql') mode = 'text/x-sql';
    return CodeMirror.resolveMode(mode).identifierQuote || '`';
  }

  function getText (item) {
    return typeof item == 'string' ? item : item.text;
  }

  function wrapTable (name, value) {
    if (isArray(value)) value = { values: value };
    if (!value.text) value.text = name;
    return value;
  }

  function parseTables (input) {
    const result = {};
    if (isArray(input)) {
      for (let i = input.length - 1; i >= 0; i--) {
        const item = input[i];
        result[getText(item).toUpperCase()] = wrapTable(getText(item), item);
      }
    } else if (input) {
      for (const name in input)
        result[name.toUpperCase()] = wrapTable(name, input[name]);
    }
    return result;
  }

  function getTable (name) {
    return tables[name.toUpperCase()];
  }

  function getColumn (name) {
    if (!columns.hasOwnProperty(name.toUpperCase())) {
      return getColumnJsonByTableName(name.toUpperCase());
    }
    return columns[name.toUpperCase()];
  }

  function shallowClone (object) {
    const result = {};
    for (const key in object) if (object.hasOwnProperty(key))
      result[key] = object[key];
    return result;
  }

  function match (string, word) {
    const len = string.length;
    const sub = getText(word).substr(0, len);
    return string.toUpperCase() === sub.toUpperCase();
  }

  function addMatches (result, search, wordlist, formatter) {
    if (isArray(wordlist)) {
      for (let i = 0; i < wordlist.length; i++)
        if (match(search, wordlist[i].trim())) result.push(formatter(wordlist[i].trim()));
    } else {
      for (const word in wordlist) if (wordlist.hasOwnProperty(word)) {
        let val = wordlist[word];
        if (!val || val === true)
          val = word.trim();
        else
          val = val.displayText ? { text: val.text.trim(), displayText: val.displayText.trim() } : val.text.trim();
        if (match(search, val)) result.push(formatter(val));
      }
    }
  }

  function cleanName (name) {
    // Get rid name from identifierQuote and preceding dot(.)
    if (name.charAt(0) == '.') {
      name = name.substr(1);
    }
    // replace doublicated identifierQuotes with single identifierQuotes
    // and remove single identifierQuotes
    const nameParts = name.split(identifierQuote + identifierQuote);
    for (let i = 0; i < nameParts.length; i++)
      nameParts[i] = nameParts[i].replace(new RegExp(identifierQuote, 'g'), '');
    return nameParts.join(identifierQuote);
  }

  function insertIdentifierQuotes (name) {
    const nameParts = getText(name).split('.');
    for (let i = 0; i < nameParts.length; i++)
      nameParts[i] = identifierQuote +
        // doublicate identifierQuotes
        nameParts[i].replace(new RegExp(identifierQuote, 'g'), identifierQuote + identifierQuote) +
        identifierQuote;
    const escaped = nameParts.join('.');
    if (typeof name == 'string') return escaped;
    name = shallowClone(name);
    name.text = escaped;
    return name;
  }

  function nameCompletion (cur, token, result, editor) {
    // Try to complete table, column names and return start position of completion
    let useIdentifierQuotes = false;
    const nameParts = [];
    let start = token.start;
    let cont = true;
    while (cont) {
      cont = (token.string.charAt(0) == '.');
      useIdentifierQuotes = useIdentifierQuotes || (token.string.charAt(0) == identifierQuote);

      start = token.start;
      nameParts.unshift(cleanName(token.string));

      token = editor.getTokenAt(Pos(cur.line, token.start));
      if (token.string == '.') {
        cont = true;
        token = editor.getTokenAt(Pos(cur.line, token.start));
      }
    }

    // Try to complete table names
    let string = nameParts.join('.');
    addMatches(result, string, tables, function (w) {
      return useIdentifierQuotes ? insertIdentifierQuotes(w) : w;
    });

    // Try to complete columns from defaultTable
    // addMatches(result, string, defaultTable, function(w) {
    //   return useIdentifierQuotes ? insertIdentifierQuotes(w) : w;
    // });

    // Try to complete columns
    string = nameParts.pop();
    let table = nameParts.join('.');

    let alias = false;
    const aliasTable = table;
    // Check if table is available. If not, find table by Alias
    if (!getTable(table)) {
      const oldTable = table;
      table = findTableByAlias(table, editor);
      if (table !== oldTable) alias = true;
    }

    let tableMatch = getTable(table);
    if (tableMatch && tableMatch.values)
      tableMatch = tableMatch.values;

    if (tableMatch) {
      addMatches(result, string, tableMatch, function (w) {
        let tableInsert = table;
        if (alias == true) tableInsert = aliasTable;
        if (typeof w == 'string') {
          w = tableInsert + '.' + w;
        } else {
          w = shallowClone(w);
          w.text = tableInsert + '.' + w.text;
        }
        return useIdentifierQuotes ? insertIdentifierQuotes(w) : w;
      });
    }

    let columnMatch = getColumn(table);
    if (columnMatch && columnMatch.values)
      columnMatch = columnMatch.values;

    if (columnMatch) {
      addMatches(result, string, columnMatch, function (w) {
        let tableInsert = table;
        if (alias == true) tableInsert = aliasTable;
        if (typeof w == 'string') {
          w = tableInsert + '.' + w;
        } else {
          w = shallowClone(w);
          w.text = tableInsert + '.' + w.text;
        }
        return useIdentifierQuotes ? insertIdentifierQuotes(w) : w;
      });
    }

    return start;
  }

  function eachWord (lineText, f) {
    const words = lineText.split(/\s+/);
    for (let i = 0; i < words.length; i++)
      if (words[i]) f(words[i].replace(/[,;]/g, ''));
  }

  function searchKeyWord (lineText) {
    const words = lineText.split(/\s+/);
    let rs = '';
    for (let i = words.length - 1; i >= 0 && rs == ''; i--) {
      if (words[i]) {
        const wordUpperCase = words[i].replace(/[,;]/g, '').toUpperCase();
        if (tablePreKeywords.includes(wordUpperCase) && i != words.length - 3) {
          rs = 'table';
        }
        if (columnPreKeywords.includes(wordUpperCase) || wordUpperCase.indexOf(CONS.LEFT_PARENTTHESIS) > -1) {
          rs = 'column';
        }
        if (wordUpperCase.trim() == '*') {
          rs = 'no';
        }
        if (CONS.ALIAS_KEYWORD == wordUpperCase && i == words.length - 2) {
          rs = 'no';
        }
        if (tablePreKeywords.includes(wordUpperCase) && i == words.length - 3) {
          rs = 'no';
        }
      }
    }

    return rs;
  }

  function findTableByAlias (alias, editor) {
    const doc = editor.doc;
    const fullQuery = doc.getValue();
    const aliasUpperCase = alias.toUpperCase();
    let previousWord = '';
    let table = '';
    const separator = [];
    let validRange = {
      start: Pos(0, 0),
      end: Pos(editor.lastLine(), editor.getLineHandle(editor.lastLine()).length),
    };

    //add separator
    let indexOfSeparator = fullQuery.indexOf(CONS.QUERY_DIV);
    while (indexOfSeparator != -1) {
      separator.push(doc.posFromIndex(indexOfSeparator));
      indexOfSeparator = fullQuery.indexOf(CONS.QUERY_DIV, indexOfSeparator + 1);
    }
    separator.unshift(Pos(0, 0));
    separator.push(Pos(editor.lastLine(), editor.getLineHandle(editor.lastLine()).text.length));

    //find valid range
    let prevItem = null;
    const current = editor.getCursor();
    for (let i = 0; i < separator.length; i++) {
      if ((prevItem == null || cmpPos(current, prevItem) > 0) && cmpPos(current, separator[i]) <= 0) {
        validRange = { start: prevItem, end: separator[i] };
        break;
      }
      prevItem = separator[i];
    }

    if (validRange.start) {
      const query = doc.getRange(validRange.start, validRange.end, false);

      for (let i = 0; i < query.length; i++) {
        const lineText = query[i];
        eachWord(lineText, function (word) {
          const wordUpperCase = word.toUpperCase();
          if (wordUpperCase === aliasUpperCase && (getTable(previousWord) || getColumn(previousWord)))
            table = previousWord;
          if (wordUpperCase !== CONS.ALIAS_KEYWORD)
            previousWord = word;
        });
        if (table) break;
      }
    }
    return table;
  }

  // parse sql, return table
  function findTable (editor) {
    const doc = editor.doc;
    const fullQuery = doc.getValue();
    const separator = [];
    const table = [];
    let validRange = {
      start: Pos(0, 0),
      end: Pos(editor.lastLine(), editor.getLineHandle(editor.lastLine()).length),
    };

    //add separator
    let indexOfSeparator = fullQuery.indexOf(CONS.QUERY_DIV);
    while (indexOfSeparator != -1) {
      separator.push(doc.posFromIndex(indexOfSeparator));
      indexOfSeparator = fullQuery.indexOf(CONS.QUERY_DIV, indexOfSeparator + 1);
    }
    separator.unshift(Pos(0, 0));
    separator.push(Pos(editor.lastLine(), editor.getLineHandle(editor.lastLine()).text.length));

    //find valid range
    let prevItem = null;
    const current = editor.getCursor();
    for (let i = 0; i < separator.length; i++) {
      if ((prevItem == null || cmpPos(current, prevItem) > 0) && cmpPos(current, separator[i]) <= 0) {
        validRange = { start: prevItem, end: separator[i] };
        break;
      }
      prevItem = separator[i];
    }

    if (validRange.start) {
      const query = doc.getRange(validRange.start, validRange.end, false);

      for (let i = query.length - 1; i >= 0; i--) {
        const lineText = query[i];
        let containsKeyword = false;
        eachWord(lineText, function (word) {
          const wordUpperCase = word.toUpperCase();
          if (containsKeyword) {
            containsKeyword = false;
            table.push(wordUpperCase);
          }
          if (tablePreKeywords.includes(wordUpperCase)) {
            containsKeyword = true;
          }
        });
      }
    }

    return table;
  }

  // parse sql, return the last keywords
  function parseSql (editor) {
    const doc = editor.doc;
    const fullQuery = doc.getValue();
    const separator = [];
    let nextWordType = '';
    let validRange = {
      start: Pos(0, 0),
      end: Pos(editor.lastLine(), editor.getLineHandle(editor.lastLine()).length),
    };

    //add separator
    let indexOfSeparator = fullQuery.indexOf(CONS.QUERY_DIV);
    while (indexOfSeparator != -1) {
      separator.push(doc.posFromIndex(indexOfSeparator));
      indexOfSeparator = fullQuery.indexOf(CONS.QUERY_DIV, indexOfSeparator + 1);
    }
    separator.unshift(Pos(0, 0));
    separator.push(editor.getCursor());

    //find valid range
    let prevItem = null;
    const current = editor.getCursor();
    for (let i = 0; i < separator.length; i++) {
      if ((prevItem == null || cmpPos(current, prevItem) > 0) && cmpPos(current, separator[i]) <= 0) {
        validRange = { start: prevItem, end: current };
        break;
      }
      prevItem = separator[i];
    }

    if (validRange.start) {
      const query = doc.getRange(validRange.start, validRange.end, false);
      const fullQuery = query.join(' ').replace(';', '').trim();
      if (fullQuery.split(/\s+/).length > 1) {
        for (let i = query.length - 1; i >= 0; i--) {
          const lineText = query[i];
          nextWordType = searchKeyWord(lineText);
          if (nextWordType != '') break;
        }
      } else {
        nextWordType = 'no';
      }
    }

    return nextWordType;
  }

  function transColumnByTables (tableArr) {
    const columnArr = new Object();
    for (let i = 0; i < tableArr.length; i++) {
      const column = getColumn(tableArr[i]);
      if (column && column.values) {
        for (let j = 0; j < column.values.length; j++) {
          columnArr[column.values[j]] = { text: column.values[j], value: [] };
        }
      }
    }
    return columnArr;
  }

  CodeMirror.registerHelper('hint', 'sql', function (editor, options) {
    tables = parseTables(options && options.tables);
    columns = parseTables(options && options.columns);
    // var defaultTableName = options && options.defaultTable;
    const disableKeywords = options && options.disableKeywords;
    // defaultTable = defaultTableName && getTable(defaultTableName);
    keywords = getKeywords(editor);
    identifierQuote = getIdentifierQuote(editor);

    // if (defaultTableName && !defaultTable)
    //   defaultTable = findTableByAlias(defaultTableName, editor);

    // defaultTable = defaultTable || [];

    // if (defaultTable.columns)
    //   defaultTable = defaultTable.columns;

    const nextWordType = parseSql(editor);

    const cur = editor.getCursor();
    const result = [];
    let token = editor.getTokenAt(cur), start, end, search;
    if (token.end > cur.ch) {
      token.end = cur.ch;
      token.string = token.string.slice(0, cur.ch - token.start);
    }

    if (token.string.match(/^[.`"\w@]\w*$/)) {
      search = token.string;
      start = token.start;
      end = token.end;
    } else {
      start = end = cur.ch;
      search = '';
    }
    if (search.charAt(0) == '.' || search.charAt(0) == identifierQuote) {
      start = nameCompletion(cur, token, result, editor);
    } else {
      // addMatches(result, search, defaultTable, function(w) {return {text:w, className: "CodeMirror-hint-table CodeMirror-hint-default-table"};});
      if (!disableKeywords && nextWordType == '')
        addMatches(result, search, keywords, function (w) { return { text: w.toUpperCase(), className: 'CodeMirror-hint-keyword' }; });

      if (nextWordType == 'table') {
        addMatches(
          result,
          search,
          tables,
          function (w) {
            if (typeof w === 'object') {
              w.className = 'CodeMirror-hint-table';
            } else {
              w = { text: w, className: 'CodeMirror-hint-table' };
            }

            return w;
          }
        );
      }

      if (nextWordType == 'column') {
        const tableArr = findTable(editor);
        if (tableArr.length > 0) {
          addMatches(
            result,
            search,
            transColumnByTables(tableArr),
            function (w) {
              if (typeof w === 'object') {
                w.className = 'CodeMirror-hint-column';
              } else {
                w = { text: w, className: 'CodeMirror-hint-column' };
              }

              return w;
            }
          );
        }
      }

      if (!disableKeywords && nextWordType != '' && nextWordType != 'no')
        addMatches(result, search, keywords, function (w) { return { text: w.toUpperCase(), className: 'CodeMirror-hint-keyword' }; });
    }

    return { list: result, from: Pos(cur.line, start), to: Pos(cur.line, end) };
  });
});
