<template>
  <div>
    <el-popover width=480 placement="bottom" trigger="click" v-model="visibel_" @show="init">
      <div class="meta-tool">
        <h2>Open to others</h2>
        <el-form :model="form" ref="form" :rules="rule"  label-width="130px" label-position="left" v-loading="createLoading">
          <el-form-item
            label="Set to Searchable"
            prop="isPublic"
          >
            <el-switch
              v-model="form.isPublic"
              active-value="1"
              inactive-value="0"
            />
          </el-form-item>
          <el-form-item
            label="Notebook Name"
            prop="title"
          >
            <!-- <el-input
              v-model="form.title"
              type="text"
              maxlength="200"
              placeholder="Notebook name"
              class="title"
            /> -->
            <span class="title">{{ form.title }}</span>
          </el-form-item>
          <el-form-item
            label="Description"
            prop="Description"
          >
            <el-input
              v-model="form.desc"
              type="textarea"
              :autosize="{minRows:3,maxRows: 6}"
              resize="none"
              placeholder="Add some description for search sql"
              class="desc"
            />
          </el-form-item>
        </el-form>
        <div class="footer">
          <el-button type="primary" @click="submit">Apply</el-button>
          <el-button plain @click="visibel_=false">Cancel</el-button>
        </div>
      </div>
      <el-button 
        slot="reference" 
        v-click-metric:NB_TOOLBAR_CLICK="{name: 'metaConfig'}"
        type="text"
        class="notebook-tool-btn" 
      >
        <i class="zeta-icon-lock zeta-icon" />
        <span class="title">{{ title }}</span>
      </el-button>
    </el-popover>
  </div>
</template>
<script lang="ts">
import { Vue, Component, Prop, Inject, Watch } from 'vue-property-decorator';
import Util from '@/services/Util.service';
import _ from 'lodash';
import { INotebook } from '@/types/workspace';
import { RestPacket } from '@/types/RestPacket';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { ZetaException } from '@/types/exception';
interface MetaItem {
  id: string;
  title: string;
  desc: string;
  isPublic: string;
  type: string;
}
function generateRestFile(nb: INotebook, path='/'): RestPacket.File {
  return {
    id: nb.notebookId,
    nt: Util.getNt(),
    content: nb.code,
    path,
    title: nb.name,
    createDt: nb.createTime,
    updateDt: nb.updateTime,
    status: '',
    preference: nb.preference ? JSON.stringify(nb.preference) : '',
    opened: 1,
    seq: nb.seq,
    nbType: nb.nbType
  } as RestPacket.File;
}
@Component({})
export default class MetaConfig extends Vue {
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Prop() title: string;
  @Prop() notebook: INotebook;
  createLoading = false;
  visibel_ = false;
  hasMeta = false;
  form: {
    title: string;
    desc: string;
    isPublic: string;
  };
  rule: {
    title: Array<any>;
  };
  constructor() {
    super();
    this.form = {
      title: '',
      desc: '',
      isPublic: '0'
    };
    this.rule = {
      title: [
        {
          message: 'please input notebook name',
          trigger: 'blur'
        },
        {
          validator: (r: any, f: string, cb: Function) => {
            if (f.trim() === '') {
              cb(
                new Error('Name cannot be blank!')
              );
            }
            else if (/[^a-zA-Z\-\.0-9_\s]/.test(f))
              cb(
                new Error(
                  'Name not valid. Only alphanumeric, _, -, . are allowed.'
                )
              );
            else cb();
          }
        }
      ]
    };
  }
  get notebookId(): string{
    return this.notebook.notebookId;
  }
  get platform(){ 
    return this.notebook.connection.alias;
  }
  get owner(){
    return this.notebook.nt;
  }
  get file(): RestPacket.File{
    return this.$store.getters.fileByNId(this.notebookId);
  }
  init(){
    this.createLoading = true;
    this.form.title = this.notebook.name;
    this.form.desc = '';
    this.notebookRemoteService.getNotebookMetaConfig(this.notebookId).then(res=>{
      this.createLoading = false;
      this.hasMeta = true;
      if(res.data){
        this.form.desc = res.data.desc;
        this.form.isPublic = res.data.isPublic;
      }
    }).catch((err: ZetaException)=>{
      this.createLoading = false;
      err.resolve();
      console.log(err);
    });
  }
  submit(){
    const params: MetaItem={
      id: this.notebookId,
      title: this.form.title,
      desc: this.form.desc,
      isPublic: this.form.isPublic,
      type: 'zeta'
    };
    const request = this.hasMeta?this.notebookRemoteService.updateNotebookMetaConfig: this.notebookRemoteService.addNotebookMetaConfig;
    request.call(this.notebookRemoteService, params).then(() => {
      this.visibel_ = false;
      const msg = this.form.isPublic==='1'?'Opened the notebook!':'Closed the notebook!';
      this.$message.success(msg);
    }).catch(err =>{
      this.$message.error('open the notebook failed');
      console.log(err);
    });
  }

}
</script>
<style lang="scss">
  .meta-tool{
    padding: 0 10px;
  }
  .title{
    color: #569ce1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: block;
  }
  .footer{
    float: right;
  }
  .notebook-tool-btn {
    position: relative;
    .zeta-icon{
        transition: .3s;
        font-size: 20px;
    }
    .title{
        font-size: 12px;
        line-height: 20px;
        transition: .3s;
        position: absolute;
        display: none;
    }
    &:hover{
        .zeta-icon{
            margin-right: 30px;
        }
        .title{
            transform: translateX(-30px);
            display: inline-block;
        }
    }
}
</style>