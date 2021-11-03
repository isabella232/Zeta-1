<template>
	<el-popover
		placement="bottom"
		width="800"
		@show="onShow"
		trigger="click"
		v-model="show">
		<div>
			<h3>New Sample Query</h3>
			<el-form ref="form" :model="temp_query" label-width="80px">
				<el-form-item label="Platform" v-if="options">
					<el-select v-model="temp_query.platforms" multiple placeholder="Platfroms">
						<el-option
							v-for="(item,$i) in options"
							:key="$i"
							:label="item"
							:value="item">
						</el-option>
					</el-select>
				</el-form-item>
				 <el-form-item label="Title">
					<el-input v-model="temp_query.title"></el-input>
				</el-form-item>
				<el-form-item label="Description">
					<quill-editor v-model="temp_query.desc" :options="editorOptions" />
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="onSubmit">Submit</el-button>
					<el-button @click="onCancel">Cancel</el-button>
				</el-form-item>
			</el-form>
		</div>
		<el-button slot="reference">New Sample Query</el-button>
	</el-popover>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import { MetadataSubView, SampleQuery } from '@/components/WorkSpace/Metadata';
import _ from 'lodash';
import { quillEditor } from 'vue-quill-editor'
import { RICH_TEXT_EDITOR_OPTION } from '.';
@Component({
  components: {
	  quillEditor
  },
})
export default class NewSampleQuery extends Vue{
	
	@Inject('tableName') tableName:string
	@Inject('platformOptions') platformOptions:string[]


	show:boolean = false;
	temp_query:SampleQuery = <SampleQuery>{
		platforms:[] as string[]
	}
	readonly editorOptions = RICH_TEXT_EDITOR_OPTION;

	get options(){
		return this.platformOptions
	}
	getDefaultQuery(){
		let platfroms:string[] = []
		if(this.options){
			platfroms = this.options
		}
		return <SampleQuery>{
			tableName:this.tableName || '',
			platforms:platfroms,
			title:"",
			desc:"",
			deleteFlag: 0,
			show:false
		}
	}
	onSubmit(){
		this.$emit('addSampleQuery',this.temp_query);
		this.show = false;
	}
	onCancel(){
		this.temp_query = this.getDefaultQuery();
		this.show = false;
	}
	onShow(){
		this.temp_query = this.getDefaultQuery();
	}

}
</script>

<style lang="scss" scoped>
* {
	/deep/ .ql-snow{
		.ql-formats,
		.ql-toolbar, &.ql-toolbar{
			&:after {
				display: block !important;
			}
		}
	}

	/deep/ .quill-editor{
		min-height: 250px;
	}
	/deep/ .ql-toolbar.ql-snow{
		line-height: normal;
	}
}
.quill-editor{
		/deep/ .ql-container{
		height: 200px;
	}
}
</style>
