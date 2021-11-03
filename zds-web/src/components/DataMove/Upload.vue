<template>
<div class="upload-container">
    <div class="table-name row">
        <UploadButton
          ref="file"
          :has-file="hasFile"
          @show-schema-dialog="showSchemaDialog"
          @remove-file="removeFile"/>
        <div v-if="uploadtable.schema.length>0" class="schema">
          <p>
              File Schema <i class="zeta-icon-edit" @click="editSchema"/>
          </p>
          <el-table size="mini" :data="uploadtable.schema">
            <el-table-column prop="name" label="name"/>
            <el-table-column prop="type" label="type">
              <template slot-scope="scope">
                {{ scope.row.type }}
                <template v-if="scope.row.length1!==null">
                  ({{ scope.row.length1 }}
                  <template v-if="scope.row.length2!== null">
                    ,{{ scope.row.length2 }}
                  </template>)
                </template>
              </template>
            </el-table-column>
          </el-table>
        </div>
    </div>
    <upload-dialog
      :visible.sync="schemaDialogVisible"
      :edit="edit"
      :schema="uploadtable.schema"
      @remove-file="removeFile"/>
</div>
</template>

<script lang="ts">
import { Component, Vue, Emit, Inject, Watch, Prop } from 'vue-property-decorator';
import Util from '@/services/Util.service';
import DMRemoteService from '@/services/remote/DataMove';
import config from '@/config/config';
import _ from 'lodash';
import UploadButton from './upload-dialog/upload-button.vue';
import UploadDialog from './upload-dialog/upload-dialog.vue';
import DataMove from './DataMove.vue';
import { Mode } from './internal';
// import DataMove from './DataMove.vue';
@Component({
  components: { UploadButton, UploadDialog }
})
export default class UploadTable extends Vue {
  @Inject('dmRemoteService') dmRemoteService: DMRemoteService;
  @Prop() uploadtable: DataMove.UploadFileTable;
  
  schemaDialogVisible = false;
  hasFile = false;
  edit = false;

  get uploadBtnRef() {
    return this.$refs.file as UploadButton;
  }

  get sourceValid() {
    let params = this.getParams();
    if(params.schema && params.path) {
      return true;
    } else {
      return false;
    }
  }
  
  editSchema(){
    this.schemaDialogVisible = true;
    this.edit = true;
  }

  removeFile(){
    this.uploadtable.path = '';
    this.uploadtable.schema = [];
    this.hasFile = false;
    this.edit = false;

    if(this.uploadBtnRef) {
      this.uploadBtnRef.emptyFileList();
    }
    
  }

  showSchemaDialog(form: any){
    this.edit = false;
    this.hasFile = true;
    this.schemaDialogVisible = true;
    this.uploadtable.schema = form.schema;
    this.uploadtable.path = form.path;
  }

  arrayToMap(schema: any){
    const obj: any = {};
    _.map(schema,(item: any, index: any)=>{
      let type = item.type;
      if(item.length1!==null && item.length2!==null){
        type = item.type+'('+item.length1+','+item.length2+')';
      }else if(item.length1!==null){
        type = item.type+'('+item.length1+')';
      }
      obj[item.name] = type;
    });
    return obj;
  }

  getParams() :DataMove.UploadParam {
    const params = {
      schema: this.arrayToMap(this.uploadtable.schema),
      path: this.uploadtable.path
    };
    return params;
  }

  destroyed() {
    this.reset();
    this.$emit("file-change", false);
  }

  reset() {
      this.removeFile();
  }

  @Watch('hasFile')
  handleFileChange(val: boolean){
    this.$emit("file-change", val)
  }

  @Watch("sourceValid")
  isSourceValid(val: boolean) {
      this.$emit("is-table-valid", {mode: Mode.UPLOAD, valid: val});
  }

}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.upload-container {
    .el-dialog__wrapper {
      
        & /deep/ .el-dialog__header {
            padding: 30px 30px 0px 30px;
        }

        & /deep/ .el-dialog__body {
            margin: 15px 30px 0 30px;
            padding: 0 0 30px 0;
            overflow: hidden;
            
            max-height: 550px;
            .dialog-child {
                max-height: 520px;
                overflow-y: auto;
            }
        }
    }
}
.schema::-webkit-scrollbar {
    width: 5px;
}
.schema::-webkit-scrollbar-thumb {
    border-radius: 5px;
    background-color: #c1c1c1;
}
.schema{
    max-height: 180px;
    overflow: scroll;
    p{
        margin: 5px 0;
    }
    .zeta-icon-edit{
        margin-left: 10px;
        cursor: pointer;
        &:hover{
            color: $zeta-global-color;
        }
    }
    /deep/ .el-table--mini th,
    /deep/ .el-table--mini td{
            padding: 6px 0;
        }
}
    
</style>


