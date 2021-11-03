<template>
    <el-dialog title="Rename File" :visible.sync="visible" @close="close" width="720px">
        <el-form :rules="rules" :model="form" ref="form" label-width="130px" label-position="left">
            <el-form-item label="Notebook name" prop="name" :error="nameError">
                <el-input v-model="form.name"></el-input>
            </el-form-item>
            <el-form-item label="Folder" prop="path">
                <el-input v-model="form.path"></el-input>
            </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
            <el-button plain @click="close">Cancel</el-button>
            <el-button type="primary" @click="confirm">Confirm</el-button>
        </div>
    </el-dialog>
</template>


<script lang="ts">
import { Component, Vue, Prop, Provide, Emit } from 'vue-property-decorator';
import { Form } from 'element-ui'
import { IRenameOption } from '.';
import RepoPluginBase from '../plugin-base';
import { IFile } from '@/types/repository';
import { ZetaException } from '@/types/exception';
@Component({
  components: {
  },
})

export default class RenameFileDialog extends RepoPluginBase {
    public file: IFile
    public form: IRenameOption 
    public visible: boolean = true;
    nameError = '';
    public rules = {
      name: [
        {
          // required: true,
          message: "please input notebook name",
          trigger: "blur"
        },
        {
          validator: (r: any, f: string, cb: Function) => {
            if (/[^a-zA-Z\-\.0-9_\s]/.test(f))
              cb(
                new Error(
                  "File name invalid. Only alphanumeric, _, -, . are allowed."
                )
              );
            else cb();
          }
        }
      ],
      path: [
        {
          validator: (r: any, f: string, cb: Function) => {
            if (/[^a-zA-Z\/\-\.0-9_]/.test(f)) {
                cb(
                    new Error(
                    "Folder name invalid. Only alphanumeric, _, -, . are allowed."
                    )
                );
            }
            else if(/^[^\/]/.test(f)){
                cb(
                    new Error(
                    "Folder name invalid. Folder name must start with '/'."
                    )
                );
            } else if(/[^/]$/.test(f)) {
                cb(
                    new Error(
                    "Folder name invalid. Folder name must end with '/'."
                    )
                )
            } else cb()
          }
        }
      ]
    }
    close() {
      console.log('close');
        // this.visible = false;
        this.onCancel()
    }
    confirm() {
        (this.$refs['form'] as Form).validate(valid => {
            if(valid) {
                // this.visible = false;
                this.nameError = '';
                this.repoApi.renameNotebook(this.file, this.form).then(file => {
                  this.onSuccess(file)
                }).catch(e => {
                  if (e.code === 'EXIST_NOTEBOOK') {
                    e.resolve();
                    this.nameError = e.message;
                  }
                  if(e.originalResponse) {
                    const response = e.originalResponse;
                    const isDuplicateName = response.data.status === 'EXPECTATION_FAILED' && response.data.message === 'duplicate file name';
                    if(isDuplicateName) {
                      e.resolve();
                      this.nameError = response.data.message;
                    }
                  }
                  this.onError(e);
                })
            }
        })

    }

}
</script>
