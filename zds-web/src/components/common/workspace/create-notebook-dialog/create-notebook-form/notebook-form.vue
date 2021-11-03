<template>
  <div class="notebook-form">
    <NotebookInfoForm
      ref="infoForm"
      v-model="noteInfo"
      :folder-options="folderOptions"
    />
    <NotebookOptionForm
      v-if="displayOptionForm"
      v-model="noteExtra"
      v-bind="$attrs"
      :edit="edit"
    />
  </div>
</template>

<script lang="ts">
import _ from 'lodash';
import { Component, Vue, Prop, Emit, Watch, Ref, } from 'vue-property-decorator';
import NotebookInfoForm from './notebook-info-form.vue';
import NotebookOptionForm from './notebook-option-form.vue';
import { NoteForm, NoteInfo, NoteExtra } from './note-form';
@Component({
  components: {
    NotebookInfoForm,
    NotebookOptionForm,
  },
})
export default class NotebookFrom extends Vue {
  @Prop({ default: () => [] })
  folderOptions: string[];
  @Prop()
  value: NoteForm;

  @Prop({ default: false })
  edit: boolean;

  get displayOptionForm() {
    return !this.edit || (this.edit && this.form.noteType === 'solo');
  }

  @Ref('infoForm')
  $infoForm: NotebookInfoForm;

  form: NoteForm = {
    name: '',
    folder: '',
    nameError: '',
    interpreter: 'Spark SQL',
    noteType: 'solo',
    platform: '',
  };

  @Watch('value', {immediate: true, deep: true })
  onValueChange(newVal: NoteForm, oldVal: NoteForm) {
    if (!_.isEmpty(newVal) && !_.isEqual(newVal, oldVal)) {
      this.$set(this, 'form', newVal);
    }
  }
  get noteInfo(): NoteInfo {
    return this.form as NoteInfo;
  }
  set noteInfo(info: NoteInfo) {
    this.form.name = info.name;
    this.form.folder = info.folder;
    this.setForm(this.form);
  }

  get noteExtra(): NoteExtra {

    return this.form as NoteExtra;
  }
  set noteExtra(options: NoteExtra) {
    this.form.interpreter = options.interpreter;
    this.form.noteType = options.noteType;
    this.form.platform = options.platform;
    this.setForm(this.form);
  }

  async validate() {
    return await this.$infoForm.validate();
  }

  @Emit('input')
  // eslint-disable-next-line @typescript-eslint/no-unused-vars, @typescript-eslint/no-empty-function
  setForm(form: NoteForm) { }
}
</script>

<style lang="scss" scoped>
</style>
