<template>
	<div class="sample-query">
		<div class="filter">
			<div>
				View by Platform
				<el-select v-if="platforms && selectPlatform" v-model="selectPlatform" multiple placeholder="Platfroms">
					<el-option
						v-for="(item,$i) in platforms"
						:key="$i"
						:label="item"
						:value="item">
					</el-option>
				</el-select>
			</div>
			
			<NewSampleQuery v-show="edit" @addSampleQuery="addSampleQuery"/>
		</div> 
		<div class="items">
			<div v-for="(q,$i) in queries" :key="$i">
				<SampleQueryItem  v-if="(!q.deleteFlag || q.deleteFlag != 1) && inPlatform(q)" :edit="edit" :value="q" @input="$val => q = $val"/>
			</div>
		</div>
	</div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Emit, Provide, Inject, Watch } from 'vue-property-decorator';
import { MetadataSubView, SampleQuery } from '@/components/WorkSpace/Metadata';
import _ from 'lodash';

import NewSampleQuery from './new-sample-query.vue';
import SampleQueryItem from './sample-query-item.vue';
import { quillEditor } from 'vue-quill-editor'
import { RICH_TEXT_EDITOR_OPTION } from '.';
@Component({
  components: {
	  quillEditor,
	  NewSampleQuery,
	  SampleQueryItem
  },
})
export default class MetadataSampleQuery extends Vue implements MetadataSubView{
	readonly editorOptions = RICH_TEXT_EDITOR_OPTION;	
	@Prop()
	value:SampleQuery[]
	get queries() {
		return this.value
	}
	set queries(q: SampleQuery[]) {
		this.onValChange(q);
	}
	@Emit('input')
	onValChange(queries: SampleQuery[]){
	}
	/** revert data */
	oldQueries: SampleQuery[];
	showMap:{[k:number]:boolean} = {}

	@Prop()
	edit:boolean

	@Provide("platformOptions")
	@Prop()
	platforms:string[]

	selectPlatform:string[]

	@Watch('platforms')
	setPlatform($val:string[]){
		this.selectPlatform = $val
	}

	constructor(){
		super()
		this.selectPlatform = []
	}
	mounted() {
		this.selectPlatform = this.platforms;
		this.oldQueries = this.queries;
		
	}

	addSampleQuery(query:SampleQuery){
		// this.queries.push(query)
		this.$emit('input',[...this.queries,query])
		return this.queries
	}
	inPlatform(query:SampleQuery){
		let inSelect = false;
		_.forEach(query.platforms,(p:string) => {
			if(_.includes(this.selectPlatform,p)){
				inSelect = true;
			}
		})
		return inSelect
	}
	revert(){
		this.queries = this.oldQueries;
	}
	save(){
		this.oldQueries = _.cloneDeep(this.queries)
		return this.queries
	}
}
</script>

<style lang="scss" scoped>
.sample-query{
	height: 100%;
	padding: 5px;
	.filter{
		padding: 5px  10px;
		display: flex;
    	justify-content: space-between;
	}
	.items{
		height: calc(100% - 60px);
		overflow: auto;
	}
}



</style>
