<template>
<div id="editor-mount" :style="{ width: computed_width }">
    <!-- <div class="editor-overlay" v-show="overlayShow"></div> -->
</div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject, Emit } from 'vue-property-decorator'

import CodeMirror from 'codemirror'
import _ from 'lodash'
import { IScrollInfo, INotebook, NotebookStatus, CodeType, CodeTypes } from '@/types/workspace'

/* -- Standard addons -- */
import '@/styles/codemirror.my.scss'
/* Editor scroll bar support */
import 'codemirror/addon/scroll/simplescrollbars.js'
import '@/styles/codemirror.scrollbar.css'
/* Other addons */
import 'codemirror/addon/edit/matchbrackets'
import 'codemirror/addon/edit/closebrackets'
import 'codemirror/addon/edit/matchtags'
import 'codemirror/addon/search/match-highlighter'
/** search func */
import 'codemirror/addon/search/searchcursor'
/* SQL hint */
//import 'codemirror/addon/hint/show-hint'
//import 'codemirror/addon/hint/sql-hint'
//import 'codemirror/addon/hint/show-hint.css'

// scala highlight
import 'codemirror/mode/clike/clike'
// r 
import 'codemirror/mode/r/r'
// python
import 'codemirror/mode/python/python'

import config from '@/config/config'
import Util from '@/services/Util.service';

@Component({
  components: {
  },
})

export default class EditorBase extends Vue {
    @Prop({default: false, type: Boolean}) viewOnly: boolean
    @Prop() value: string;
    @Prop() mode: string

    get content(): string{
        return this.value || ''
    }
    /* -- Constant -- */
    // readonly default_text = sql_sample;
    readonly default_emit_timeout = 500 /* ms */;

    /* -- Props -- */
    @Prop() width?: string;

    /* -- Data -- */
    cm!: CodeMirror.Editor;     /* CodeMirror instance */
    // text: String;               /* Text cache */
    scrollInfo: IScrollInfo = { left: 0, top: 0 };
    defer_code: string;

    codeMirror:any = CodeMirror
    /* computed editor width when passed in as props */
    get computed_width(): string {
        switch(typeof this.width) {
            case "string": return this.width as string;
            case "number": return this.width + "px";
            default: return "100%";
        }
    }
    /**
     * Initialize some data in constructor
     * In old-style vue, this is done by passing a function to data field.
     */
    constructor() {
        super();
    }

    /* -- Hook CodeMirror when Vue initialize -- */
    mounted(): void {

        let self = this;

        /* mount codemirror onto DOM */
        let cm = this.$data.cm = this.codeMirror(this.$el, <CodeMirror.EditorConfiguration>{
            readOnly:this.viewOnly,
            value: this.content,
            lineNumbers: true,
            smartIndent: false,
            matchBrackets: true,
            autofocus: false,
            indentWithTabs: true,
            scrollbarStyle: 'simple',
            highlightSelectionMatches: { showToken: /\w/, annotateScrollbar: true },
            extraKeys: {"Ctrl-Space": "autocomplete"},
            gutters: ["stmt-exec"],
            lineComment: "}"
        })
        console.debug("cm",cm);
        /**
         * HACK: Copy sql mimeModes specs, of which the most important is keywords,
         * to mix mode allowing autocompletion work properly.
         * This assginment MUST be executed after CodeMirror has instantiate,
         * otherwise statement will fail to be parsed.
         * TODO: refactor to hide the logic within addon.
         */
        // this.codeMirror.mimeModes["mixed"] = this.codeMirror.mimeModes["text/x-mariadb"];

        /* Update text cache */
        cm.on('change', () => {
            this.onContentChange(this.$data.cm.getValue());
        });
        cm.on('focus', this.onFocus)
        /* SQL hint */
        const noHintKeys =
            [13,    /* enter */
             8,     /* backspace */
             32,    /* space */
             9,     /* tab */
             27,    /* ESC */
             37, 38, 39, 40,    /* arrows */
             186    /* ; */
             ];

        cm.on("keyup",  (cm:any, event:any) => {
            if (!cm.state.completionActive && /* Enables keyboard navigation in autocomplete list */
                isalpha_number(event.keyCode) && !event.ctrlKey && !event.altKey)
            {
                if (event.composed && !event.shiftKey) return;
                this.codeMirror.commands.autocomplete(cm, null, {completeSingle: false});
            }

        });

        cm.on("beforeSelectionChange", (cm: CodeMirror.Doc, obj: { ranges: Array<CodeMirror.Range>}) => {
            let head = obj.ranges[0].anchor;
            let tail = obj.ranges[0].head;

            if (head.line > tail.line || (head.line === tail.line && head.ch > tail.ch)) {
                [head, tail] = [tail, head];
            }
            this.beforeSelectionChange(cm.getRange(head, tail))
            // this.$store.dispatch("setStagedCode", {
            //     nid: this.notebook.notebookId,
            //     code: cm.getRange(head, tail)
            // })
        })

        /* HACK: CodeMirror can't detect the height of its
         * container properly immediate after mounted.
         */
        setTimeout(() => {
            cm.refresh()
        }, 500);
        this.reload()
        /* HACK: Switching notebook makes Codemirror scroll to top
         * To maintain scroll position,
         * 1. Record scroll position when deactivate a notebook tab
         * 2. Scroll to previous position when reactivate.
         * Because Vue provides no beforeDeactivate but only deactivated API,
         * 1. is doing using beforeUpdate hook in <NotebookTabs>
         */

        /* Emit from <NotebookTabs> */
        // EventBus.$on("should-update-scrollInfo", _.debounce(function() {
        //     let { left, top } = cm.getScrollInfo();
        //     if (left === 0 && top === 0) return;
        //     self.$data.scrollInfo = {
        //         left: left,
        //         top: top
        //     }
        // }, 300, { leading: true }));

        // /* Emit from <Notebook> */
        // EventBus.$on("notebook-activated", _.debounce(function() {
        //     if (!self.$data.scrollInfo) return;
        //     let { left, top } = self.$data.scrollInfo;
        //     cm.scrollTo(left, top)
        // }, 300, { leading: true }));

        // EventBus.$on("notebook-undo", () => cm.undo());
        // EventBus.$on("notebook-redo", () => cm.redo());
		// EventBus.$on("notebook-format", this.formatSql);
    }

    @Emit('input')
    onContentChange(content: string){

    }

    @Emit('beforeSelectionChange')
    beforeSelectionChange(selection: string) {

    }
    @Emit('onFocus')
    onFocus() {
    }
    @Watch('mode')
    onModeChange($val: string){
        this.reload($val)
    }
    reload($val?: string){
        if(!$val && this.mode){
            $val = this.mode || 'text/x-mysql'
        }
        this.$data.cm.setOption('mode', $val);
    }

    undo(){
        this.$data.cm.undo()
    }
    redo(){
        this.$data.cm.redo()
    }
    format(){
        this.formatSql()
    }
    focus(){
        this.$data.cm.focus()
    }
    /**
     * add mark to all the submitted scripts
     */
    markSelectionText():CodeMirror.TextMarker{
        let cm = this.$data.cm;
        let from:CodeMirror.Position = cm.getCursor(true);
        let to:CodeMirror.Position = cm.getCursor(false);
        if(from.ch == to.ch && from.line == to.line && !cm.getSelection()) {
            let lastline = cm.lastLine()
            from = <CodeMirror.Position>{
                line:0,
                ch:0
            }
            to = <CodeMirror.Position>{
                line:lastline +1,
                ch:0
            }
        }
        return cm.markText(from,to,{
            css:"background-color:rgba(148, 114, 243, 0.15)",
            readOnly:true
        })
    }
    clearTextMarker(){
        let cm = this.$data.cm;
        _.chain(cm.getAllMarks())
        .forEach((marker:CodeMirror.TextMarker)=>{
            marker.clear();
        })
        .value()
	}

	formatSql () {
		let cm = this.$data.cm;
		let range = {
			from: cm.getCursor(true),
			to: cm.getCursor(false)
		}
		// if (range.from.line === range.to.line &&
		// 	range.from.ch === range.to.ch) {
		// 	range = {
		// 		from: {line: 0, ch: 0},
		// 		to: {line: cm.lineCount(), ch: cm.lastLine()}
		// 	}
		// }
		let sql = cm.getRange(range.from, range.to);
		let formatSql = Util.SQLFormatter(sql);
        let formatSqlLines = formatSql.split('\n');
		cm.replaceRange(formatSql, range.from, range.to);
        let formatEndPos = {
            line: range.from.line + formatSqlLines.length - 1,
            ch: range.from.ch + formatSqlLines[formatSqlLines.length - 1].length
        };
        cm.setSelection(range.from, formatEndPos);
	}

    @Watch('viewOnly')
    onViewOnlyChange(newVal: boolean, oldVal: boolean) {
        if(newVal != oldVal) {
            const cm = this.$data.cm;
            const before_option = cm.options.readOnly;
            if(before_option == newVal) {
                return 
            }
            cm.setOption('readOnly', newVal)
            const after_option = cm.options.readOnly;
            console.debug(`viewOnlyChange readOnly ${before_option} => ${after_option}`, cm)
        }
    }
}

function isalpha_number(key: number) {
    return (key >= 65 && key <= 90) || (key === 189) || (key >= 48 && key <= 57);
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
</style>