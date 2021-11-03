<template>
  <div v-if="file">
    <NotebookForm
      ref="notebookForm"
      v-model="form"
      class="create-notebook-body"
      :folder-options="getfolderOptions()"
      :edit="true"
      :connected="isConnectionNB(notebookId)"
    />
    <div
      class="create-notebook-footer"
    >
      <el-button
        type="primary"
        :loading="createLoading"
        @click="createNotebook(form)"
      >
        Update
      </el-button>
      <el-button
        type="default"
        plain
        @click="createCanceled"
      >
        Cancel
      </el-button>
    </div>
  </div>
</template>
<script lang="ts">
import { Vue, Component, Inject, Emit, Prop, Ref } from 'vue-property-decorator';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import { NoteForm } from './create-notebook-form/note-form';
import { CodeType, IPreference, IConnection, Notebook, INotebook, NotebookStatus, } from '@/types/workspace';
import { IFile } from '@/types/repository';
import { ZetaException } from '@/types/exception';
import { Interpreter, NoteType } from './create-notebook-form/note-options';
import NotebookForm from './create-notebook-form/notebook-form.vue';
import PlatformPermissionProvider from './platform-permission-provider';
import FolderOptionsProvider from './folder-options-provider';
import _ from 'lodash';
import { IPacketMapper } from '@/services/mapper';

function isConnected(nb: INotebook) {
  const connectedStatus = [NotebookStatus.CONNECTING, NotebookStatus.IDLE, NotebookStatus.SUBMITTING, NotebookStatus.RUNNING, NotebookStatus.STOPPING];
  return _.includes(connectedStatus, nb.status);
}

@Component({
  components: {
    NotebookForm,
  },
  mixins: [
    PlatformPermissionProvider,
    FolderOptionsProvider,
  ]
})
export default class EditNotebook extends Vue {

  @Prop()
  file: IFile;

  @Ref('notebookForm')
  $noteForm: NotebookForm;

  form: NoteForm = {
    name: 'Untitled',
    folder: '/',
    nameError: '',
    interpreter: localStorage.getItem('createNote.interpreter') as Interpreter || 'Spark SQL',
    noteType: localStorage.getItem('createNote.noteType') as NoteType || 'solo',
    platform: localStorage.getItem('createNote.platform') || '',
  };
  createLoading = false;

  get notebookId() {
    return this.file.notebookId;
  }
  @Inject('notebookRemoteService')
  notebookRemoteService: NotebookRemoteService;
  @Inject('zeppelinApi')
  zeppelinApi: ZeppelinApi;

  @Emit('new-notebook-success')
  createSucceed() {
  }

  @Emit('new-notebook-fail')
  // eslint-disable-next-line @typescript-eslint/no-empty-function, @typescript-eslint/no-unused-vars
  createFailed(e: any) {}

  @Emit('new-notebook-canceled')
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  createCanceled() {}

  // @Watch('file', { immediate: true, deep: true })
  // onFileChange(file: IFile) {
  //   this.initForm(file);
  // }
  mounted() {
    this.initForm(this.file);
  }
  initForm(file: IFile) {
    this.form.name = file.title;
    this.form.folder = file.path;
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    const preference = file.preference!;
    const nbType = file.nbType;
    if (nbType === 'single' || nbType === 'collection') {
      this.form.noteType = 'solo';
      this.setNoteFormByPreference(this.form, preference);
    } else {
      this.form.noteType = 'stacked';
    }

  }
  setNoteFormByPreference(noteForm: NoteForm, preference: IPreference) {
    const cnn = preference['notebook.connection'];
    if (!cnn) return;
    switch (cnn.codeType) {
      case CodeType.SQL:
        noteForm.interpreter = 'Spark SQL';
        break;
      case CodeType.TERADATA:
        noteForm.interpreter = 'TD SQL';
        break;
      case CodeType.KYLIN:
        noteForm.interpreter = 'Kylin';
        break;
      case CodeType.HIVE:
        noteForm.interpreter = 'Hive SQL';
        break;
    }
  }
  /**
   *
   * 1. call rest api
   * 2. add file to repo
   * 3. add workspace
   */
  async createNotebook(noteForm: NoteForm) {
    const valid = await this.$noteForm.validate();
    if (!valid) return;
    if (!_.startsWith(noteForm.folder, '/')) {
      noteForm.folder = '/' + noteForm.folder;
    }
    if (!_.endsWith(noteForm.folder, '/')) {
      noteForm.folder = noteForm.folder + '/';
    }
    this.createLoading = true;
    let request;
    if (noteForm.noteType === 'stacked') {
      request = this.renameZeppelinNotebook(noteForm);
    } else {
      request = this.renameZetaNotebook(noteForm);
    }
    request.then((file: IFile) => {
      this.createLoading = false;
      const srcFile = _.clone(this.file);
      const nb = this.$store.getters.nbByNId(file.notebookId);
      this.$message({
        type: 'success',
        message: 'Successfully rename.',
        customClass: 'rename-success-message'
      });
      this.$store.dispatch(
        'updateFile',
        Object.assign(srcFile, file)
      );
      if (nb) {
        this.$store.dispatch(
          'updateNotebook',
          Object.assign(nb, { ...nb, name: file.title, preference: file.preference })
        );
      }
      this.createSucceed();
    }).catch((e: any) => {
      this.createLoading = false;
      // this.createFailed(e);
    });
  }

  renameZeppelinNotebook(noteForm: NoteForm) {
    const name = noteForm.folder + noteForm.name;
    return this.zeppelinApi.renameNote(this.notebookId, name)
      .then(() => {
        return {
          notebookId: this.notebookId,
          title: noteForm.name,
          path: noteForm.folder,
          preference: this.file.preference
        } as IFile;
      })
      .catch((e: ZetaException) => {
        if(e.originalResponse) {
          const response = e.originalResponse;
          const isDuplicateName = response.data.status === 'EXPECTATION_FAILED' && response.data.message === 'duplicate file name';
          if(isDuplicateName) {
            e.resolve();
            this.form.nameError = 'duplicate file name';
          }
        }
        throw e;
      });
  }
  renameZetaNotebook(noteForm: NoteForm) {
    const file = _.clone(this.file);
    const codeType = this.parseZetaCodeType(noteForm);
    file.title = noteForm.name;
    file.path = noteForm.folder;
    if (!file.preference) {
      file.preference = {} as IPreference;
    }
    if (!file.preference['notebook.connection']) {
      file.preference['notebook.connection'] = {} as IConnection;
    }
    file.preference['notebook.connection'] = { codeType } as IConnection;
    return this.notebookRemoteService.rename(IPacketMapper.fileMapper(file))
      .then(() => file)
      .catch((e: ZetaException) => {
        if (e.code === 'EXIST_NOTEBOOK') {
          e.resolve();
          this.form.nameError = 'duplicate file name';
        }
        throw e;
      });
  }
  parseZetaCodeType(form: NoteForm): CodeType {
    const interpreter = form.interpreter;
    let codeType = CodeType.SQL;
    switch (interpreter) {
      case 'TD SQL':
        codeType = CodeType.TERADATA;
        break;
      case 'Hive SQL':
        codeType = CodeType.HIVE;
        break;
      case 'Kylin':
        codeType = CodeType.KYLIN;
        break;
    }
    return codeType;
  }

  isConnectionNB(nId: string) {
    if (nId) {
      const nb: Notebook = this.$store.getters.nbByNId(nId);
      if (nb && isConnected(nb)) {
        return true;
      }
    }
    return false;
  }
}

</script>

<style lang="scss" scoped>
.create-notebook-footer{
  margin: 0 -30px;
  padding: 10px 30px 30px 30px;
  text-align: right;
}
</style>
