<template>
  <el-dialog
    title="Rename File"
    :visible.sync="visible_"
    width="520px"
    @close="close"
  >
    <el-form
      ref="form"
      :rules="rules"
      :model="form"
      label-width="130px"
      label-position="left"
    >
      <el-form-item
        label="Package Name"
        prop="name"
      >
        <el-input v-model="form.name" />
      </el-form-item>
    </el-form>
    <div
      slot="footer"
      class="dialog-footer"
    >
      <el-button
        plain
        @click="close"
      >
        Cancel
      </el-button>
      <el-button
        v-loading="loading"
        type="primary"
        @click="confirm"
      >
        Confirm
      </el-button>
    </div>
  </el-dialog>
</template>


<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from 'vue-property-decorator';
import { Form } from 'element-ui';
import { ZetaException } from '@/types/exception';
import { IHDFSFile } from '@/types/workspace';
import HDFSRemoteService from '@/services/remote/HDFSRemoteService';
import _ from 'lodash';

interface RenamePackageOption {
  name: string;
}

@Component({
  components: {},
})
export default class RenamePackageDialog extends Vue {
  @Inject('HDFSRemoteService') HDFSRemoteService: HDFSRemoteService;
  @Prop() visible: boolean;
  @Prop() currentPackage: IHDFSFile;
  @Prop() cluster: number;
  public form: RenamePackageOption;
  public loading = false;
  constructor () {
    super();
    this.form = {
      name: '',
    };
  }
  public validateName = (r: any, value: any, cb: Function) => {
    if (!_.trim(value)) cb(new Error('please input package name'));
    else if (_.trim(value).length > 90) cb(new Error('max length 90'));
    else if (/[^a-zA-Z\-\.0-9_]/.test(value))
      cb(
        new Error(
          'File name not valid. Only alphanumeric, _, -, . are allowed.'
        )
      );
    else cb();
  };
  public rules = {
    name: [
      {
        validator: this.validateName,
        trigger: 'blur',
      },
    ],
  };
  set visible_ (e) {
    this.$emit('update:visible', e);
  }
  get visible_ (): boolean {
    return this.visible;
  }
  get folder (){
    return this.$store.getters.folder;
  }
  close () {
    this.visible_ = false;
  }
  confirm () {
    (this.$refs['form'] as Form).validate(valid => {
      if (valid) {
        this.loading = true;
        const name = this.form.name + '.' + this.currentPackage.type;
        const dstPath = this.folder + this.form.name + '.' + this.currentPackage.type;
        const srcPath = this.folder + this.currentPackage.fileName;
        this.HDFSRemoteService.rename(
          srcPath,
          dstPath,
          this.currentPackage.cluster!
        )
          .then(res => {
            this.loading = false;
            this.visible_ = false;
            const data = res.data;
            if (data.code === 500) {
              this.$message({
                type: 'warning',
                message: data.msg?data.msg: 'name is exist',
                customClass: 'rename-fail-message',
              });
            } else {
              this.$message({
                type: 'success',
                message: 'Successfully rename.',
                customClass: 'rename-success-message',
              });
              this.$store.dispatch(
                'updatePackage',
                Object.assign(this.currentPackage, { fileName: name })
              );
              this.$emit('rename-success');
            }
          })
          .catch((e: ZetaException) => {
            this.loading = false;
            e.message = 'Fail to rename. See console for detail';
          });
      }
    });
  }
  @Watch('visible')
  handleVisible (newVal: boolean) {
    if (newVal) {
      this.form = {
        name: this.currentPackage.fileName.substring(
          0,
          this.currentPackage.fileName.lastIndexOf('.')
        ),
      };
    }
  }
}
</script>
