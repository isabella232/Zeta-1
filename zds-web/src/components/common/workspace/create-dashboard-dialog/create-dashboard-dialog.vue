<template>
  <el-dialog 
    label-width="80px"
    :title="dialogTitle" 
    :visible.sync="visible_"
    class="crt-dashboard-dialog"
  >
    <el-form :rules="rule" :model='form' ref='form'>
      <!-- <el-form-item label="Folder" prop="folder"> 
          <el-input v-model="form.folder"></el-input>
      </el-form-item>
      <el-form-item label="Name" prop="name">
          <el-input v-model="form.name"></el-input>
      </el-form-item> -->
      <el-form-item label="Dashboard Name" prop="name">
        <el-input v-model="form.name"></el-input>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="newNotebook">Create</el-button>
      <el-button plain @click="visible_ = false">Cancel</el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts">
/**
 * Component <NewNotebookDialogue>. Popup dialogue for creation of new notebook.
 * Used by <NotebookTabs> and <RepoList>.
 */
import { Component, Vue, Prop, Watch, Inject } from 'vue-property-decorator';
import {
  CodeTypes,
  IDashboardFile,
  LayoutConfig
} from '@/types/workspace';
import DashboardRemoteService from '@/services/remote/Dashboard';
import _ from 'lodash';

function postComputePosition(items: LayoutConfig[]): LayoutConfig[] {
  const configs = _.chain(items)
    .sortBy(['y', 'x'])
    .map((item: LayoutConfig, $i) => {
      item.i = $i;
      return item;
    })
    .value();
  console.debug('postComputePosition', configs);
  return configs;
}
@Component({
  components: {}
})
export default class NewDashboardDialog extends Vue {
  @Inject('dashboardRemoteService')
  dashboardRemoteService: DashboardRemoteService;

  @Prop() visible: boolean;
  @Prop() cloneDb: boolean;
  @Prop() cloneDashboard: any;
  private dialogTitle = 'Create Dashboard';
  set visible_(e) {
    this.$emit('update:visible', e);
  }
  get visible_(): boolean {
    return this.visible;
  }
  get interpreterOptions(): string[] {
    return _.chain(CodeTypes)
      .keys()
      .value();
  }
  form: {
    name: string;
  };

  rule: {
    name: Array<any>;
    interpreter?: Array<any>;
  };

  constructor() {
    super();
    this.form = {
      name: 'Untitled'
    };
    this.rule = {
      name: [
        {
          required: true,
          message: 'please input notebook name',
          trigger: 'blur'
        },
        {
          validator: (r: any, f: string, cb: Function) => {
            if (/[^a-zA-Z\-\.0-9_\/]/.test(f))
              cb(
                new Error(
                  'Folder name not valid. Only alphanumeric, _, -, . are allowed.'
                )
              );
            else cb();
          }
        }
      ],
      interpreter: [
        {
          required: true,
          message: 'please select interpreter',
          trigger: 'blur'
        }
      ]
    };
  }

  newNotebook() {
    this.dashboardRemoteService.createDashboard(this.form.name)
      .then(({ data: db }) => {
        const dbDict = {
          id: db.id,
          nt: db.nt,
          name: db.name,
          layoutConfigs: db.layoutConfig
            ? (JSON.parse(db.layoutConfig) as LayoutConfig[])
            : [],
          createTime: db.createDt,
          updateTime: db.updateDt ? db.updateDt : null
        } as IDashboardFile;
        this.$store.dispatch('addDashboard', { dashboard: dbDict });
        /* update dashboard */
        if (this.cloneDb && this.cloneDashboard != undefined && this.cloneDashboard != null) {
          const id: string = db.id;
          const items = postComputePosition(this.cloneDashboard.layoutConfigs);
          this.dashboardRemoteService.updateDashboard(
            id,
            JSON.stringify(items)
          ).then(() => {
            this.$store.dispatch('updateWorkspace', {
              layoutConfigs: items,
              notebookId: id
            });
            this.$store.dispatch('updateDashboard', {
              layoutConfigs: items,
              id: id
            });
          // this.loading = false;
          });
          this.$emit('clone-dashboard-success', {});
        }
        this.form.name = '';
        this.visible_ = false;
        this.$message.success('Successfully created dashboard on server.');
      })
      .catch(e => {
        e.message = 'Fail to create dashboard';
      });
  }
  @Watch('cloneDb')
  onChangeClone(newVal: boolean, oldVal: boolean){
    if(newVal){
      this.dialogTitle = 'Clone Dashboard';
      this.form.name = this.cloneDashboard.name+'_clone';
    }
  }
  @Watch('visible_')
  onVisibleChange(newVal: boolean,oldVal: boolean){
    if(!this.cloneDb && newVal === true && newVal !== oldVal){
      this.dialogTitle = 'Create Notebook';
      this.form.name = 'Untitled';
    }
  }
}
</script>

<style lang="scss" scoped>
.crt-notebook-dialog {
  /deep/ .interpreter-select {
    width: 100%;
    // /deep/ .el-input{

    // }
  }
}
</style>
