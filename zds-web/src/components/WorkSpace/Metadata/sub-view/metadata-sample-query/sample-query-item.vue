<template>
    <div class="sample-query-item">
        <div class="sample-query-item-delete" v-if="edit" >
            <el-button type="text" class="del-btn" icon="el-icon-remove" @click="deleteQuery" />
        </div>
        <div class="sample-query-item__body">
            <div class="sample-query-title" :class="{'close':!show}" @click="edit ? '': show = !show">
                <div class="content">
                    <template v-if="!edit">{{sampleQuery.title}}</template>
                    <el-input v-else :value="sampleQuery.title" @input="$val => onValChange('title',$val)"/>

                </div>
                <div class="toggle-btn">
                    <el-button type="text" :icon="show ? 'el-icon-arrow-down': 'el-icon-arrow-right'" @click="!edit ? '': show = !show"/> 
                </div>
            </div>
            <div class="sample-query-desc" v-show="show">
                    <div v-if="!edit" class="sample-query-desc-display" v-html="sampleQuery.desc"></div>
                    <quill-editor v-else :value="sampleQuery.desc" @input="$val => onValChange('desc',$val)" :options="editorOptions" />
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject, Watch } from 'vue-property-decorator';
import { MetadataSubView, SampleQuery } from '@/components/WorkSpace/Metadata';
import _ from 'lodash';
import { quillEditor } from 'vue-quill-editor'
import { RICH_TEXT_EDITOR_OPTION } from '.';
@Component({
  components: {
	  quillEditor
  },
})
export default class SampleQueryItem extends Vue{
	readonly editorOptions = RICH_TEXT_EDITOR_OPTION;	
	@Prop()
    value:SampleQuery
    
    @Prop({default:false})
    edit:boolean

    show:boolean
    constructor(){
        super()
        this.show = false
    }
    get sampleQuery(){
        return this.value
    }

    /** on val Change */
    onValChange(type:string,$val:any){
        if(type === 'title'){
            this.value.title = $val;
        }
        else if(type === 'desc'){
            this.value.desc = $val;
        }
        else if(type === 'deleteFlag'){
            this.value.deleteFlag = $val;
        }
        this.$emit('input',this.value);
    }
    deleteQuery(){
         this.$confirm(`Confirm to delete ${this.value.title}`, 'Warning', {
          confirmButtonText: 'Confirm',
          cancelButtonText: 'Cancel',
          type: 'warning'
        }).then(() => {
            this.onValChange('deleteFlag',1);
        }).catch(() => {      
        });
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
}
.quill-editor{
		/deep/ .ql-container{
		height: 200px;
	}
}
.sample-query-item{
    // width: 100%;
    // max-width: 800px;
    margin: 5px 15px;
    display: flex;
    .sample-query-item-delete{
        align-self: center;
        width: 40px;
        text-align: right;
        .del-btn{
            color: #999;
            font-size: 20px;
        }
    }
    .sample-query-item__body{
        flex-grow: 1;
        // border:1px solid rgba(221, 221, 221, 1);
        // border-radius: 5px;
        // padding: 3px 8px;
        .sample-query-title{
            display: flex;
            justify-content: space-between;
            background-color: rgba(243, 244, 246, 1);
            // margin: -3px -8px -3px -8px;
            padding: 3px 8px 3px 8px;
            border-top: 1px solid #ccc;
            border-left: 1px solid #ccc;
            border-right: 1px solid #ccc;
            &.close{
                border-bottom: 1px solid #ccc;
            }
            .content{
                width: calc(100% - 50px);
                font-size: 14px;
                font-family: 'Helvetica-Bold', 'Helvetica Bold', 'Helvetica';
                font-weight: 700;
                align-self: center;
            }
        }
        .sample-query-desc{
            .sample-query-desc-display{
                padding: 5px 10px;
                max-height: 250px;
                overflow: auto;

                border-bottom: 1px solid #ccc;
                border-left: 1px solid #ccc;
                border-right: 1px solid #ccc;
            }
            // padding: 5px 10px;
        }
    }
    
}
</style>
