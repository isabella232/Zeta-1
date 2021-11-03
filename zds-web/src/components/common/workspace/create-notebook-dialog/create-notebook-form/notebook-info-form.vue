<template>
  <el-form
    v-if="subForm"
    ref="form"
    :rules="rule"
    label-position="left"
    label-width="130px"
    :model="subForm"
  >
    <el-form-item
      label="Notebook Name"
      prop="name"
      :error="errorMessage"
    >
      <el-input
        id="newNBName"
        :value="subForm.name"
        @input="onNameChange"
      />
    </el-form-item>
    <el-form-item
      label="Folder"
      prop="folder"
    >
      <el-autocomplete
        :value="subForm.folder"
        class="block-input"
        :fetch-suggestions="queryFolder"
        @input="onFolderChange"
      />
    </el-form-item>
  </el-form>
</template>

<script lang="ts">
import _ from 'lodash';
import { Component, Vue, Prop, Emit, Watch, Ref } from 'vue-property-decorator';
import { NoteInfo } from './note-form';
import { Form as ElForm }  from 'element-ui';

@Component({
  components: {
  }
})
export default class NotebookInfoForm extends Vue {
  @Prop({ default: [] })
  folderOptions: string[];

  @Prop()
  value: NoteInfo;

  @Ref('form')
  $form: ElForm;

  subForm: NoteInfo = {
    name: '',
    folder: '',
    nameError: '',
  };

  readonly rule = {
    name: [
      {
      // required: true,
        message: 'please input notebook name',
        trigger: 'blur'
      },
      {
        validator: (r: any, f: string, cb: Function) => {
          if (f.trim() === '') {
            cb(
              new Error('File name cannot be blank!')
            );
          }
          else if (/[^a-zA-Z\-\.0-9_\s]/.test(f))
            cb(
              new Error(
                'File name not valid. Only alphanumeric, _, -, . are allowed.'
              )
            );
          else cb();
        }
      }
    ],
    folder: [
      {
        validator: (r: any, f: string, cb: Function) => {
          if (/[^a-zA-Z\/\-\.0-9_]/.test(f))
            cb(
              new Error(
                'Folder name not valid. Only alphanumeric, _, -, . are allowed.'
              )
            );
          else cb();
        }
      }
    ]
  };

  get errorMessage() {
    const msg = this.value.nameError || '';
    return msg;
  }
  @Watch('value', {immediate: true, deep: true })
  onValueChange(newVal: NoteInfo, oldVal: NoteInfo) {
    if (!_.isEmpty(newVal) && !_.isEqual(newVal, oldVal)) {
      this.$set(this, 'subForm', newVal);
    }
  }
  onNameChange(name: string) {
    this.subForm.name = name;
    this.onFormChange(this.subForm);
  }
  onFolderChange(folder: string) {
    this.subForm.folder = folder;
    this.onFormChange(this.subForm);
  }

  @Emit('input')
  // eslint-disable-next-line @typescript-eslint/no-unused-vars, @typescript-eslint/no-empty-function
  onFormChange(form: NoteInfo) {

  }
  queryFolder(folder: string, cb: Function){
    const results: any = [];
    _.forEach(this.folderOptions, path =>{
      if(path.indexOf(folder)>-1){
        const obj = { value: path };
        results.push(obj);
      }
    });
    cb(results);
  }

  validate() {
    this.value.nameError = '';
    return new Promise((resovle) => {
      this.$form.validate((valid) => {
        resovle(valid);
      });
    });
  }
}

</script>

<style lang="scss" scoped>

</style>
