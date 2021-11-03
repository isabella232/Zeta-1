<template>
  <div
    id="editor-mount"
    v-loading="convertSQLLoading"
    :class="viewOnly || isPublic ? 'read-only-code-mirror': null"
    :style="{ width: computed_width, fontSize: fontSize }"
    @mouseup="forceFocus"
  >
    <!-- <div class="editor-overlay" v-show="overlayShow"></div> -->
  </div>
</template>

<script lang="ts">
/**
 * Component <Editor>. Codemirror wrapper as a notebook editor.
 *
 * @prop notebook:INotebook Current notebook.
 * @event should-update-scrollInfo Event informs editor to record scroll position in store.
 * @event notebook-activated Notebook activated and brought to front.
 * @event notebook-undo Event suggests undo.
 * @event notebook-redo Event suggests redo.
 */
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import CodeMirror from 'codemirror';
import SqlConverterRemoteService from '@/services/remote/SQLConverter';
import _ from 'lodash';
import { IScrollInfo, INotebook, NotebookStatus, CodeType, CodeTypes } from '@/types/workspace';

/* -- Standard addons -- */
import '@/styles/codemirror.my.scss';
/* Editor scroll bar support */
import 'codemirror/addon/scroll/simplescrollbars.js';
import '@/styles/codemirror.scrollbar.css';
/* Other addons */
import 'codemirror/addon/edit/matchbrackets';
import 'codemirror/addon/edit/closebrackets';
import 'codemirror/addon/edit/matchtags';
import 'codemirror/addon/search/match-highlighter';
/** search func */
import 'codemirror/addon/search/searchcursor';
/* SQL hint */
import 'codemirror/addon/hint/show-hint';
//import 'codemirror/addon/hint/sql-hint'
import './sql-hint.js';
import './show-hint.css';
import 'codemirror/addon/hint/show-hint.css';
import 'codemirror/addon/comment/comment';

// scala highlight
import 'codemirror/mode/clike/clike';
/* -- Custom addon -- */
// import './cm-addon'
import './cm-custom-mime';

import Util from '@/services/Util.service';
import { ZetaException, ExceptionPacket } from '@/types/exception';
import { Getter } from 'vuex-class';
import * as Logger from '@/plugins/logs/zds-logger';
import { tables, columns } from './autoCompleteJson';

function isalpha_number (key: number) {
  return (key >= 65 && key <= 90) || (key === 189) || (key >= 48 && key <= 57);
}

function isComment (editor: any) {
  const doc = editor.doc;
  const currentRow = {
    start: CodeMirror.Pos(editor.getCursor().line, 0),
    end: CodeMirror.Pos(editor.getCursor().line, editor.getCursor().ch),
  };

  // determine line comments by "-- "
  const currentLine = doc.getRange(currentRow.start, currentRow.end, false);
  const currentLineQuery = currentLine && currentLine[0] ? currentLine[0] : '';
  // determine block comments by "/* */"
  const remainQuery = doc.getValue().lastIndexOf('*/') > -1 ? doc.getValue().substr(doc.getValue().lastIndexOf('*/') + 2) : doc.getValue();
  return (currentLineQuery.indexOf('-- ') > -1) || remainQuery.indexOf('/*') > -1;
}
// import vClickOutside from "v-click-outside";
@Component({
  components: {
  },
  // directives: {
  //   clickOutside: vClickOutside.directive
  // }
})

export default class Editor extends Vue {
  @Inject() viewOnly: boolean;
  @Inject('sqlConverterRemoteService')
  sqlConverterRemoteService: SqlConverterRemoteService;


  @Getter('userPreferenceByKey') userPreference: (key: string) => any;
  @Prop() notebook: INotebook;

  /* -- Constant -- */
  // readonly default_text = sql_sample;
  readonly default_emit_timeout = 500 /* ms */;

  // convert loading
  private convertSQLLoading = false;

  /* -- Props -- */
  @Prop() width?: string;

  /* -- Data -- */
  cm!: CodeMirror.Editor;     /* CodeMirror instance */
  // text: String;               /* Text cache */
  scrollInfo: IScrollInfo = { left: 0, top: 0 };
  defer_code: string;
  focused: boolean;

  codeMirror: any = CodeMirror;
  /* computed editor width when passed in as props */
  get computed_width (): string {
    switch (typeof this.width) {
      case 'string': return this.width as string;
      case 'number': return this.width + 'px';
      default: return '100%';
    }
  }
  get isPublic () {
    return this.notebook.publicReferred != null;
  }
  get fontSize () {
    return (this.userPreference('editor-font-size') || 14) + 'px';
  }
  get isOS (){
    return Util.getOS() === 'MAC' ? true: false;
  }

  /* -- Hook CodeMirror when Vue initialize -- */
  mounted (): void {
    /* mount codemirror onto DOM */
    const cm = this.$data.cm = this.codeMirror(this.$el, {
      readOnly:this.viewOnly || this.isPublic ? true : false,
      value: this.notebook.code,
      lineNumbers: true,
      smartIndent: false,
      matchBrackets: true,
      autofocus: true,
      indentUnit: 4,
      indentWithTabs: true,
      scrollbarStyle: 'simple',
      highlightSelectionMatches: { showToken: /\w/, annotateScrollbar: true },
      // mode: config.defaultCodeType.mime,
      // extraKeys: {"Ctrl-Space": "autocomplete"},
      gutters: ['stmt-exec'],
    } as CodeMirror.EditorConfiguration);
    console.info(`NB{${this.notebook.notebookId}}, CM`, cm);
    /**
         * HACK: Copy sql mimeModes specs, of which the most important is keywords,
         * to mix mode allowing autocompletion work properly.
         * This assginment MUST be executed after CodeMirror has instantiate,
         * otherwise statement will fail to be parsed.
         * TODO: refactor to hide the logic within addon.
         */
    // this.codeMirror.mimeModes["mixed"] = this.codeMirror.mimeModes["text/x-mariadb"];

    /* Update text cache */
    cm.on('change', (instance, e: CodeMirror.EditorChangeCancellable) => {
      if (e.origin === 'setValue') {
        return;
      }
      this.$emit('change', {
        history: this.$data.cm.getDoc().historySize(),
        code: this.$data.cm.getValue(),
      });
    });

    /* SQL hint */
    const noHintKeys =
            [13,    /* enter */
              8,     /* backspace */
              32,    /* space */
              9,     /* tab */
              27,    /* ESC */
              37, 38, 39, 40,    /* arrows */
              186,    /* ; */
            ];

    cm.on('keyup',  (cm: any, event: any) => {
      const autoComplete = _.isUndefined(this.userPreference('auto-complete')) ? true : this.userPreference('auto-complete');
      const token = cm.getTokenAt(cm.getCursor());
      const input = token.string || '';
      if (!cm.state.completionActive && /* Enables keyboard navigation in autocomplete list */
                !isComment(cm) && /* determine the current input is not a comment */
                (isalpha_number(event.keyCode) || event.keyCode == 190) && event.keyCode != 48 && !event.ctrlKey && !event.altKey && autoComplete && input.indexOf('\'') == -1)
      {
        // if (event.composed && !event.shiftKey) return;
        this.codeMirror.commands.autocomplete(cm, this.codeMirror.hint.sql, {

          // "completeSingle: false" prevents case when you are typing some word
          // and in the middle it is automatically completed and you continue typing by reflex.
          // So user will always need to select the intended string
          // from popup (even if it's single option). (copy from @Oleksandr Pshenychnyy)
          completeSingle: false,

          // there are 2 ways to autocomplete field name:
          // (1) table_name.field_name (2) field_name
          // Put field name in table names to autocomplete directly
          // no need to type table name first.
          tables: tables,
          //columns: autoComplete.columns
          columns: columns,
        });
      }

    });

    cm.on('beforeSelectionChange', (cm: CodeMirror.Doc, obj: { ranges: Array<CodeMirror.Range>}) => {
      let head = obj.ranges[0].anchor;
      let tail = obj.ranges[0].head;

      if (head.line > tail.line || (head.line === tail.line && head.ch > tail.ch)) {
        [head, tail] = [tail, head];
      }

      this.$store.dispatch('setStagedCode', {
        nid: this.notebook.notebookId,
        code: cm.getRange(head, tail),
      });
    });

    cm.on('blur', () => {
      this.focused = false;
    });
    cm.on('focus', () => {
      this.focused = true;
    });
    cm.addKeyMap({
      'Shift-Tab': ()=>{
        Logger.counter('NB_TOOLBAR_CLICK', 1, { name: 'indent', trigger: 'keyEvent'});
        cm.execCommand('indentLess');
      },
    });
    if (this.isOS){
      cm.addKeyMap({
        'Cmd-/': ()=>{
          Logger.counter('NB_TOOLBAR_CLICK', 1, { name: 'linecomments', trigger: 'keyEvent'});
          this.lineComment();
        },
        'Cmd-Ctrl-/': ()=>{
          Logger.counter('NB_TOOLBAR_CLICK', 1, { name: 'blockcomments', trigger: 'keyEvent'});
          this.blockComment();
        },
        'Shift-Cmd-U': ()=>{
          Logger.counter('NB_TOOLBAR_CLICK', 1, { name: 'lowercase', trigger: 'keyEvent'});
          this.lowerCase();
        },
      });
    } else {
      cm.addKeyMap({
        'Ctrl-/': ()=>{
          Logger.counter('NB_TOOLBAR_CLICK', 1, { name: 'linecomments', trigger: 'keyEvent'});
          this.lineComment();
        },
        'Shift-Ctrl-/': ()=>{
          Logger.counter('NB_TOOLBAR_CLICK', 1, { name: 'blockcomments', trigger: 'keyEvent'});
          this.blockComment();
        },
        'Shift-Ctrl-U': ()=>{
          Logger.counter('NB_TOOLBAR_CLICK', 1, { name: 'lowercase', trigger: 'keyEvent'});
          this.lowerCase();
        },
      });
    }

    /* HACK: CodeMirror can't detect the height of its
         * container properly immediate after mounted.
         */
    setTimeout(() => {
      cm.refresh();
    }, 500);
    this.$el.addEventListener('wheel', this.wheelEventHandler, { passive: false });
    this.onEditorWrapperUpdate();
    /* HACK: Switching notebook makes Codemirror scroll to top
         * To maintain scroll position,
         * 1. Record scroll position when deactivate a notebook tab
         * 2. Scroll to previous position when reactivate.
         * Because Vue provides no beforeDeactivate but only deactivated API,
         * 1. is doing using beforeUpdate hook in <NotebookTabs>
         */

    // EventBus.$on("notebook-undo", () => cm.undo());
    // EventBus.$on("notebook-redo", () => cm.redo());
    // EventBus.$on("notebook-format", this.formatSql);
  }

  public saveScrollInfo () {
    const { left, top } = this.$data.cm.getScrollInfo();
    // console.log('save scroll', 'left: ' +left, 'top: ' + top)
    this.$data.scrollInfo = {
      left: left,
      top: top,
    };
  }

  get overlayShow () {
    switch (this.notebook.status) {
      case NotebookStatus.SUBMITTING:
      case NotebookStatus.RUNNING:
      case NotebookStatus.STOPPING:
        return true;
      default:
        return false;
    }
  }

  get currentCodeType () {
    return this.$store.getters.currentCodeType;
  }
  forceFocus () {

    //   console.info("EDITOR DEBUG === force focus");
    this.$data.cm.display.input.blur();
    setTimeout(() => {
      this.$data.cm.display.input.focus();
    }, 0);

  }
  activated () {
    if (!this.$data.scrollInfo) return;
    const { left, top } = this.$data.scrollInfo;
    this.$data.cm.scrollTo(left, top);
  }
  onEditorWrapperUpdate (){
    const codeType = this.currentCodeType? this.currentCodeType : CodeType.SQL;
    this.$data.cm.setOption('mode', CodeTypes[codeType].mime);
  }
  /**
     * add mark to all the submitted scripts
     */
  markSelectionText (): CodeMirror.TextMarker{
    const cm = this.$data.cm;
    let from: CodeMirror.Position = cm.getCursor(true);
    let to: CodeMirror.Position = cm.getCursor(false);
    if (from.ch == to.ch && from.line == to.line && !cm.getSelection()) {
      const lastline = cm.lastLine();
      from = {
        line:0,
        ch:0,
      } as CodeMirror.Position;
      to = {
        line:lastline +1,
        ch:0,
      } as CodeMirror.Position;
    }
    //! manually keep editor's history
    //! history will be clear after exec function `markText`
    const his = cm.getHistory();
    const marker = cm.markText(from, to, {
      css:'background-color:rgba(148, 114, 243, 0.15)',
      readOnly:true,
    });
    cm.setHistory(his);
    return marker;
  }
  clearTextMarker (){
    const cm = this.$data.cm;
    _.chain(cm.getAllMarks())
      .forEach((marker: CodeMirror.TextMarker)=>{
        marker.clear();
      })
      .value();
  }

  undo (){
    const cm = this.$data.cm;
    cm.undo();
  }
  redo (){
    const cm = this.$data.cm;
    cm.redo();
  }

  formatSql () {
    const cm = this.$data.cm;
    const range = {
      from: cm.getCursor(true),
      to: cm.getCursor(false),
    };
      // if (range.from.line === range.to.line &&
      // 	range.from.ch === range.to.ch) {
      // 	range = {
      // 		from: {line: 0, ch: 0},
      // 		to: {line: cm.lineCount(), ch: cm.lastLine()}
      // 	}
      // }
    const sql = cm.getRange(range.from, range.to);
    const formatSql = Util.SQLFormatter(sql);
    const formatSqlLines = formatSql.split('\n');
    cm.replaceRange(formatSql, range.from, range.to);
    const formatEndPos = {
      line: range.from.line + formatSqlLines.length - 1,
      ch: range.from.ch + formatSqlLines[formatSqlLines.length - 1].length,
    };
    cm.setSelection(range.from, formatEndPos);
  }
  UpperSQLReserved (){
    const cm = this.$data.cm;
    const range = {
      from: cm.getCursor(true),
      to: cm.getCursor(false),
    };
    const sql = cm.getRange(range.from, range.to);
    const s = Util.UpperSQLReserved(sql);
    cm.replaceRange(s, range.from, range.to);
    cm.setSelection(range.from, range.to);
  }
  indent (){
    const cm = this.$data.cm;
    cm.execCommand('defaultTab');
  }
  outdent (){
    const cm = this.$data.cm;
    cm.execCommand('indentLess');
  }
  upperCase (){
    const cm = this.$data.cm;
    const range = {
      from: cm.getCursor(true),
      to: cm.getCursor(false),
    };
    const sql = cm.getRange(range.from, range.to);
    const s = sql.toUpperCase();
    cm.replaceRange(s, range.from, range.to);
    cm.setSelection(range.from, range.to);
  }
  lowerCase (){
    const cm = this.$data.cm;
    const range = {
      from: cm.getCursor(true),
      to: cm.getCursor(false),
    };
    const sql = cm.getRange(range.from, range.to);
    const s = sql.toLowerCase();
    cm.replaceRange(s, range.from, range.to);
    cm.setSelection(range.from, range.to);
  }
  lineComment (){
    const cm = this.$data.cm;
    cm.toggleComment({lineComment: '--'});
  }
  blockComment (){
    const cm = this.$data.cm;
    const range = {
      from: cm.getCursor(true),
      to: cm.getCursor(false),
    };
    let mode: string | null = null;
    if (mode == null) {
      if (cm.uncomment(range.from, range.to)) mode = 'un';
      else { cm.blockComment(range.from, range.to); mode = 'block'; }
    } else if (mode == 'un') {
      cm.uncomment(range.from, range.to);
    } else {
      cm.blockComment(range.from, range.to);
    }
  }
  sqlConvert () {
    const cm = this.$data.cm;
    const range = {
      from: cm.getCursor(true),
      to: cm.getCursor(false),
    };
    const sql = cm.getRange(range.from, range.to);

    if (sql) {
      this.convertSQL(sql).then((sql: any) => {
        cm.replaceRange(sql, range.from, range.to);
        this.$emit('convert');
      });
    } else {
      // this.$message({
      //     showClose: true,
      //     message: `No SQL script found`,
      //     type: "warning"
      // });
    }
  }

  convertSQL (SQL: string) {
    this.convertSQLLoading = true;
    return this.sqlConverterRemoteService.manualconvertsql(SQL)
      .then((response: any) => {
        if (response.data.errorMessage) {
          const ex = new ZetaException( {
            code: '',
            errorDetail: {
              message: response.data.errorMessage,
            },
          } as ExceptionPacket, {
            path: 'notebook',
            workspaceId: this.notebook.notebookId,
          });
          this.$store.dispatch('addException', { exception: ex});
          throw ex;
        } else {
          this.convertSQLLoading = false;
          return response.data.convertSql;
        }
      })
      .catch(error => {
        this.convertSQLLoading = false;
        throw error;
      });
  }

  setCodeValue (text: string) {
    if (this.$data.cm) {
      this.$data.cm.setValue(text);
    }
  }

  wheelEventHandler(ev: WheelEvent) {
    if (Math.abs(ev.deltaX) > Math.abs(ev.deltaY)) {
      ev.preventDefault();
    }
  }
  // @Watch('notebook')
  // onNotebookChange(nb: INotebook, old_nb: INotebook) {
  //     if (this.cm) this.cm.setValue(nb.code)
  // }

}


</script>
<style lang="scss" scoped>
#editor-mount {
    position: relative;
    .editor-overlay {
        height: 100%;
        width: 100%;
        z-index: 1000;
        background: rgba(0, 0, 0, 0);
        position: absolute;
    }
}
.editor {
    height: 100%;
    border: 1px solid #dddddd;
    box-sizing: border-box;
}
</style>
