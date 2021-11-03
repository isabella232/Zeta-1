<template>
	<div class="find-sql" >
		<el-button type="text" icon="el-icon-search" class="find-btn" :class="{'active':show}" @click=" show= !show" style="display:none"></el-button>
		<div class="popup" v-show="show" :style="style">
			<div class="find">

				<el-input ref="searchInput" class="replace-input" v-model="replace.source"  @keyup.enter.native="()=> searchEvent()">
					<i slot="prefix" class="el-input__icon el-icon-search"></i>
				</el-input>
				<el-button-group class="action">
					<el-button @click="searchEvent">Find</el-button>
					<el-button @click="clearEvent">clear</el-button>
				</el-button-group>
			</div>
			<div class="replace">
				<el-input class="replace-input" v-model="replace.target">
				</el-input>
				<el-button-group class="action">
					<el-button @click="() => replaceEvent()">Replace</el-button>
					<el-button @click="() => replaceAllEvent()">ReplaceAll</el-button>
				</el-button-group>
			</div>
		</div>
	</div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import CodeMirror from "codemirror";
import { IPreference, INotebook } from "@/types/workspace";

class SearchState {
  posFrom: any;
  posTo: any;
  lastQuery: any;
  query: any;
  overlay: any;
  [k: string]: any;
  constructor() {
    this.posFrom = this.posTo = this.lastQuery = this.query = null;
    this.overlay = null;
  }
}
function getSearchState(cm: any) {
  return cm.state.search || (cm.state.search = new SearchState());
}

function queryCaseInsensitive(query: string) {
  return typeof query == "string" && query == query.toLowerCase();
}

function getSearchCursor(cm: any, query: string, pos?: any) {
  // Heuristic: if the query string is all lowercase, do a case insensitive search.
  return cm.getSearchCursor(query, pos, {
    caseFold: queryCaseInsensitive(query),
    multiline: true
  });
}
function searchOverlay(query: any, caseInsensitive: boolean) {
  if (typeof query == "string")
    query = new RegExp(
      query.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&"),
      caseInsensitive ? "gi" : "g"
    );
  else if (!query.global)
    query = new RegExp(query.source, query.ignoreCase ? "gi" : "g");

  return {
    token: function(stream: any) {
      query.lastIndex = stream.pos;
      var match = query.exec(stream.string);
      if (match && match.index == stream.pos) {
        stream.pos += match[0].length || 1;
        return "searching";
      } else if (match) {
        stream.pos = match.index;
      } else {
        stream.skipToEnd();
      }
    }
  };
}
function parseString(string: string) {
  return string.replace(/\\(.)/g, function(_: any, ch: any) {
    if (ch == "n") return "\n";
    if (ch == "r") return "\r";
    return ch;
  });
}
function parseQuery(query: any) {
  var isRE = query.match(/^\/(.*)\/([a-z]*)$/);
  if (isRE) {
    try {
      query = new RegExp(isRE[1], isRE[2].indexOf("i") == -1 ? "" : "i");
    } catch (e) {} // Not a regular expression after all, do a string search
  } else {
    query = parseString(query);
  }
  if (typeof query == "string" ? query == "" : query.test("")) query = /x^/;
  return query;
}
@Component({
  components: {}
})
export default class FindSql extends Vue {
  @Prop() notebookId: string;
  replace = {
    source: "",
    target: ""
  };
  show:boolean = false;
  get editor() {
    return this.$parent.$parent.$refs.editor as any;
  }
  get cm(): any {
    let editor = this.$parent.$parent.$refs.editor as any;
    if (editor && editor._data && editor._data.cm) {
      return editor._data.cm;
    }
  }
  get preference (): IPreference | undefined {
    const notebook: INotebook = this.$store.state.workspace.workspaces[
      this.notebookId
    ];
    if (notebook) {
      return notebook.preference;
    } else {
      return undefined;
    }
  }
  get layout (){
    const currentLayout =  this.preference && this.preference['notebook.layout'] ?
      this.preference['notebook.layout']:{};
    return currentLayout;
  }
  get style () {
    const layout = this.layout;
    const position = layout.display==='vertical'?`calc((100% - ${layout.vertical}%) + 5px)`:'5px';
    return {
      right: position,
    };
  }
  mounted() {
  }
  clearEvent(){
	  this.replace = {
		  source:"",
		  target:""
	  }
	  this.clearOverlay()
  }
  replaceEvent() {
	  let query = this.replace.source;
    if(this.cm && query){
      this.doSearch(query)
      this.replaceFunc(false)
    }
  }
  replaceAllEvent(){
    let query = this.replace.source;
    let text = this.replace.target;
    if(this.cm && query){
      this.doSearch(query)
      this.replaceAll(query,text)
    }
  }
  searchEvent() {
    if (!this.cm) {
		return
    }
	  let query = this.replace.source;
	  let pos : CodeMirror.Position = this.cm.getCursor()
	  // set overlay
	this.doSearch(query)
	//set selection
	this.selectSearch(query,pos)



  }
  selectSearch(query: string,pos?:CodeMirror.Position) {

    let cursor = this.cm.getSearchCursor(query,pos);
    let state = getSearchState(this.cm);
	if(cursor ){
		if(cursor.findNext()){
			this.cm.setSelection(cursor.from(),cursor.to())
		}
		else {
			let newPos:CodeMirror.Position = {line:0,ch:0}
			cursor = this.cm.getSearchCursor(query,newPos);
			if(cursor.findNext()){
				this.cm.setSelection(cursor.from(),cursor.to())
			}
		}
	}

  }
  doSearch(query:string){
	this.cm.operation(()=>{
		let state = getSearchState(this.cm);
      state.queryText = query;
      state.query = parseQuery(query);
      this.cm.removeOverlay(state.overlay, queryCaseInsensitive(state.query));
      state.overlay = searchOverlay(
        state.query,
        queryCaseInsensitive(state.query)
      );
      this.cm.addOverlay(state.overlay);
      if (this.cm.showMatchesOnScrollbar) {
        if (state.annotate) {
          state.annotate.clear();
          state.annotate = null;
        }
        state.annotate = this.cm.showMatchesOnScrollbar(
          state.query,
          queryCaseInsensitive(state.query)
        );
      }
	  })

  }
   replaceFunc( all:boolean) {
	   let cm = this.cm
    if (cm.getOption("readOnly")) return;
    var query = this.replace.source;


      if (!query) return;
      query = parseQuery(query);
        let text = parseString(this.replace.target)

        //   clearSearch(cm);
          var cursor = getSearchCursor(cm, query, cm.getCursor("from"));
          var advance = function() {
            var start = cursor.from(), match:any;
            if (!(match = cursor.findNext())) {
              cursor = getSearchCursor(cm, query);
              if (!(match = cursor.findNext()) ||
                  (start && cursor.from().line == start.line && cursor.from().ch == start.ch)) return;
            }
            cm.setSelection(cursor.from(), cursor.to());
            cm.scrollIntoView({from: cursor.from(), to: cursor.to()});
			 cursor.replace(typeof query == "string" ? text :
                           text.replace(/\$(\d)/g, function(_, i) {return match[i];}));
            if(all)        advance();
          };
          var doReplace = function(match:any) {
            cursor.replace(typeof query == "string" ? text :
						   text.replace(/\$(\d)/g, function(_, i) {return match[i];}));
          };
          advance();

  }
  replaceAll(query:string, text:string) {
    this.cm.operation(() => {
      for (var cursor = getSearchCursor(this.cm, query); cursor.findNext();) {
        if (typeof query != "string") {
          var match = this.cm.getRange(cursor.from(), cursor.to()).match(query);
          cursor.replace(text.replace(/\$(\d)/g, function(_, i) {return match[i];}));
        } else cursor.replace(text);
      }
    });
  }
  clearOverlay() {
    this.cm.operation(() => {
      var state = getSearchState(this.cm);
      state.lastQuery = state.query;
      if (!state.query) return;
      state.query = state.queryText = null;
      this.cm.removeOverlay(state.overlay);
      if (state.annotate) {
        state.annotate.clear();
        state.annotate = null;
      }
    });
  }
  // API
  open(word?:string){
    if(word){
      this.replace.source = word
    }

    this.show = true;
    // (this.$refs['searchInput'] as any).focus()
    setTimeout(()=>{
      let input = this.$refs['searchInput'] as any
      if(input){
        input.focus()
      }
    },100)
  }
  close(){
    this.clearEvent();
    this.show = false;
  }
  toggle(word?:string){
    if(this.show && !word && !this.replace.source){
      this.close();
    }
    else{
      this.open(word)
    }
  }
}
</script>
<style lang="scss" scoped>
.find-btn{
	padding: 3px;
	cursor:pointer;
	&.active{
		border: 1px solid;
		// background-color: #336e7b;
		color: #FFF;
	}
}
.replace-input {
  	width: 200px;
  	/deep/ .el-input__inner{
		  height: 25px;
		  line-height: 25px;
	  }
	.el-input__icon{
		line-height: 25px;
	}
}
.popup{
	top: 50px;
  right: 5px;
  position: absolute;
	z-index: 999;

	border: 1px solid;
  padding: 3px;
	background-color: #FFF;
	.find,
	.replace{
		display:flex;
		justify-content: space-between;
		align-items: center;
		.action{
			margin-left: 30px;
			display:inline-flex;
			justify-content: flex-end;
			align-items: center;
			.el-button{
				padding: 5px 8px;
				font-size: 12px;
			}
		}
	}
}
</style>


