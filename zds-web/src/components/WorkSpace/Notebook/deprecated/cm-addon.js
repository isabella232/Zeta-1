/* -- Custom Codemirror addon -- 
 * Add support for 
 * 1. SQL keywords highlight
 * 2. SQL statement parsing. 
 *    statements range stored in CodeMirror.state.stmtsRange: Array[{ start: number, end: number }]
 * 3. Background highlighted SQL statement at cursor.
 * 4. TODO: Add support for statement multiple selection via gutter button.
 */ 

import CodeMirror from "codemirror"
import _ from 'lodash'
import 'codemirror/mode/sql/sql'
import "codemirror/addon/mode/simple"
import "codemirror/addon/mode/overlay"

/**
 * Mode for parsing SQL statement
 * This mode use semicolon (;) as statement delimiter 
 */
CodeMirror.defineSimpleMode("sql-stmt", {
    start: [
        { regex: /--.*/, token: "comment" },
        { regex: /\/\*/, token: "comment", next: "comment" },
        { regex: /\s/ },
        { regex: /.*?;/, token: "statement-tail"},
        { regex: /.*/, token: "statement"}],
    comment: [
        { regex: /.*?\*\//, token: "comment", next: "start" },
        { regex: /.*/, token: "comment" }
    ],
    meta: {
        name: "sql", helperType: "sql"
    }
})

CodeMirror.defineMode("mixed", function(config, _) {
    let mode = CodeMirror.overlayMode(
        /* Do not break the order. i.e. sql-stmt should be placed before text/x-mysql */
        CodeMirror.getMode(config, "sql-stmt"),
        CodeMirror.getMode(config, "text/x-mysql")
    )
    mode.helperType = "mixed";
    mode.name = "sql";
    return mode;
})

/* Assign mariadb's keywords to mixed mode for auto completion */
// CodeMirror.mimeModes["mixed"] = CodeMirror.mimeModes["text/x-mariadb"];

/* TODO: refactor highlight statement at cursor as a service,
 * may be implemented as a class.
 */

function makeMarker(cm, n) {
    let marker = document.createElement("div");
    marker.style.height = cm.defaultTextHeight() * n + "px";
    marker.innerHTML = "";
    marker.classList.add("stmt-button-1");
    return marker;
}

function addStmtButton(cm) {
    cm.clearGutter('stmt-button');
    console.log(cm.state.stmtsRange)
    cm.state.stmtsRange.forEach(({ start, end }) => {
        let info = cm.lineInfo(start);
        cm.setGutterMarker(start, "stmt-button", info.gutterMarkers ? null : makeMarker(cm, end - start + 1));
    })
}

function traceSQLStmt(cm) {
    console.log("traceSQLStmt");

    function getStmtsRange(cm) {
        /* TODO: comment */
        const NORMAL = 0, STMT = 1, STMT_TAIL = 2;
        const prior = {
            "statement": STMT,
            "statement-tail": STMT_TAIL
        };

        let line_states = 
            _.range(cm.lineCount())
            .map(line => Math.max.apply(null, cm.getLineTokens(line).map(({ type }) => type in prior ? prior[type] : NORMAL)));
            
        let stmts = [/* Stmt stored as { start, end } */];
        let in_stmt = false, start;
        for(let i = 0; i < line_states.length; i++) {
            let state = line_states[i];
            if (i == 0) {
                if (state >= STMT) {
                    in_stmt = true;
                    start = 0;
                    if (state === STMT_TAIL) {
                        stmts.push({ start: 0, end: 0 });
                    }
                }
            }
            else {
                if (in_stmt) {
                    if (state === STMT_TAIL) {
                        in_stmt = false;
                        stmts.push({ start, end: i });
                    }
                }
                else {
                    if (state === STMT_TAIL) {
                        stmts.push({ start: i, end: i });
                    }
                    else if (state === STMT) {
                        in_stmt = true;
                        start = i;
                    }
                }
            }
        }

        return stmts;
    }

    cm.state.stmtsRange = getStmtsRange(cm);
    CodeMirror.signal(cm, "stmt-range-ready", null);
    highlightStmtAtCursor(cm);
    // addStmtButton(cm);
}

let highlightStmtAtCursor = (function() {
    /* static store current highlighted range */
    let highlightedRange = null;
    let highlightedWidget = null;

    return _.debounce(function(cm) {

        function highlightRange(range, cursorLine) {
            let lineHeight = cm.defaultTextHeight();
            let el = document.createElement('div');
            el.classList.add("highlighted-statement");
            el.style.top = "-" + ((cursorLine - range.start) * lineHeight) + "px";
            el.style.height = (range.end - range.start + 1) * lineHeight + "px";
            highlightedWidget = cm.addLineWidget(cursorLine, el, { above: true });
        }
    
        function removeHighlightedRange() {
            highlightedWidget.clear();
            highlightedWidget = null;
        }

        if (highlightedWidget) {
            removeHighlightedRange();
        }

        let stmtsRange = cm.state.stmtsRange;
        let cursorLine = cm.getCursor().line;
        
        if (cursorLine) {

            let stmtIndex = null;
            
            for(let i = 0; i < stmtsRange.length; i++) {
                if (stmtsRange[i].start <= cursorLine && cursorLine <= stmtsRange[i].end) {
                    stmtIndex = i;
                    break;
                }
            }

            if (stmtIndex !== null) {
                highlightedRange = stmtsRange[stmtIndex];
                highlightRange(highlightedRange, cursorLine);
            }

        }

    }, 100);

})();

CodeMirror.defineExtension("traceSQLStmt", traceSQLStmt);

CodeMirror.defineOption("SQLStmtSensitive", true, function(cm, val, old) {

    if (val) {
        traceSQLStmt(cm);
        /* BUG: change/changes events sometimes triggered before mode parsing */
        cm.on("changes", traceSQLStmt);
        cm.on("cursorActivity", highlightStmtAtCursor);
    }
    else {
        cm.off("change", traceSQLStmt);
    }

});
