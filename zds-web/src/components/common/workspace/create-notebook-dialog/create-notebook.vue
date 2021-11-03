<template>
  <div>
    <NotebookForm
      ref="notebookForm"
      v-model="form"
      class="create-notebook-body"
      :folder-options="getfolderOptions()"
    />
    <div
      class="create-notebook-footer"
    >
      <el-button
        type="primary"
        :loading="createLoading"
        :disabled="isDisabledPlatform(form.platform, form.noteType)"
        @click="createNotebook(form)"
      >
        Create
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
import uuid from 'uuid';
import moment from 'moment';
import { Vue, Component, Inject, Emit, Prop, Ref, Watch } from 'vue-property-decorator';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
import { WorkspaceSrv as NBSrv } from '@/services/Workspace.service';
import { NoteForm } from './create-notebook-form/note-form';
import { RestPacket } from '@/types/RestPacket';
import { CodeType, IPreference, ZPLNote, IConnection, IWorkspace } from '@/types/workspace';
import { IFile } from '@/types/repository';
import Util from '@/services/Util.service';
import { ZetaException } from '@/types/exception';
import { WorkspaceManager } from '@/types/workspace/workspace-manager';
import { Interpreter, NoteType } from './create-notebook-form/note-options';
import NotebookForm from './create-notebook-form/notebook-form.vue';
import PlatformPermissionProvider from './platform-permission-provider';
import FolderOptionsProvider from './folder-options-provider';
import _ from 'lodash';

@Component({
  components: {
    NotebookForm,
  },
  mixins: [
    PlatformPermissionProvider,
    FolderOptionsProvider,
  ]
})
export default class CreateNotebook extends Vue {
  @Prop()
  folder: string;

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

  @Inject('notebookRemoteService')
  notebookRemoteService: NotebookRemoteService;
  @Inject('zeppelinApi')
  zeppelinApi: ZeppelinApi;

  @Emit('new-notebook-success')
  createSucceed() {
    this.$message({
      type: 'success',
      message: 'Successfully created notebook on server.',
      customClass: 'crt-success-message'
    });
  }

  @Emit('new-notebook-fail')
  // eslint-disable-next-line @typescript-eslint/no-empty-function, @typescript-eslint/no-unused-vars
  createFailed(e: any) {}

  @Emit('new-notebook-canceled')
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  createCanceled() {}

  @Watch('folder', { immediate: true })
  onFileChange(folder: string) {
    this.form.folder = folder;
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
      request = this.createZeppelinNotebook(noteForm);
    } else {
      request = this.createZetaNotebook(noteForm);
    }
    request.then((id: string) => {
      this.createLoading = false;
      this.addFile(id, noteForm);
      this.addWorkspace(id, noteForm);
      this.createSucceed();
      // this.form.name = 'Untitled';
    }).catch((e: any) => {
      this.createLoading = false;
      // this.createFailed(e);
    });
  }
  createZetaNotebook(noteForm: NoteForm, seq = -1, opened = -1): Promise<any> {
    const codeType = this.parseZetaCodeType(noteForm);
    const preference = {
      'notebook.connection': {
        codeType,
      }
    } as IPreference;
    const packet: RestPacket.File = {
      id: uuid(),
      nt: Util.getNt(),
      content: '',
      path: this.parseFolder(noteForm.folder),
      title: noteForm.name,
      createDt: moment().valueOf(),
      updateDt: moment().valueOf(),
      preference: JSON.stringify(preference),
      status: '',
      seq,
      opened,
      nbType: 'single',
    };
    return this.notebookRemoteService.add(packet)
      .then(({ data }) => {
        return data.id;
      })
      .catch((e: ZetaException) => {
        if (e.code === 'EXIST_NOTEBOOK') {
          e.resolve();
          this.form.nameError = 'duplicate file name';
        }
        throw e;
      });
  }
  createZeppelinNotebook(noteForm: NoteForm) {
    const zplNote = this.parseZeppelinNote(noteForm);
    return this.zeppelinApi.createNote(zplNote)
      .then(({ data }) => {
        return data.body;
      }).catch((e: ZetaException) => {
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

  addFile(id: string, noteForm: NoteForm) {
    const file = {
      notebookId: id,
      title: noteForm.name,
      nt: Util.getNt(),
      content: '',
      path: noteForm.folder,
      createTime: moment().valueOf(),
      updateTime: moment().valueOf(),
      state: '',
      preference: this.getPreference(noteForm),
      nbType: noteForm.noteType === 'solo' ? 'single' : 'zeppelin',
    } as IFile;
    this.$store.dispatch('addFile', file);
  }

  addWorkspace(id: string, noteForm: NoteForm) {
    let nb: IWorkspace;
    if (noteForm.noteType === 'solo') {
      nb = NBSrv.notebook({
        notebookId: id,
        name: noteForm.name,
        preference: this.getPreference(noteForm),
        nbType: 'single' ,
      });
    } else {
      nb = NBSrv.zeppelinNotebook({
        notebookId: id,
        name: noteForm.name,
        preference: this.getPreference(noteForm),
        nbType: 'zeppelin' ,
      });
    }
    WorkspaceManager.getInstance(this).addActiveTabAndOpen(nb);
    return nb;
  }
  getPreference(noteForm: NoteForm) {
    const cnn = {
      alias:noteForm.platform,
    } as IConnection;
    if (noteForm.noteType === 'solo') {
      const codeType = this.parseZetaCodeType(noteForm);
      cnn.codeType = codeType;
    }
    return {
      'notebook.connection': cnn,
    } as IPreference;
  }
  parseFolder(path: string) {
    if (path && path.length > 0) {
      const lastChar = path[path.length - 1];
      if (lastChar !== '/') {
        path = path + '/';
      }
      const firstChar = path[0];
      if (firstChar !== '/') {
        path = '/' + path;
      }
    }
    return path;
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
  parseZeppelinNote(form: NoteForm): ZPLNote {
    const magicMap = {
      'Spark SQL': 'sql',
      'PySpark': 'pyspark',
      'Scala': 'scala',
      'SparkR': 'sparkr',
    };
    const interpreterIdMap = {
      'Spark SQL': 'livy',
      'PySpark': 'livy',
      'Scala': 'livy',
      'SparkR': 'livy',
      'Kylin': 'kylin',
      'Python': 'python',
    };
    const name = form.name.trim();
    const path = this.parseFolder(form.folder);
    //! hardcode for hermes interpreter
    if (form.interpreter === 'Spark SQL' && form.platform === 'hermes') {
      return {
        name: path + name,
        defaultInterpreterId: 'hermes'
      } as ZPLNote;
    }
    const magic = magicMap[form.interpreter];
    const defaultInterpreterId = interpreterIdMap[form.interpreter];
    return {
      name: path + name,
      paragraphs: [{
        text: magic ? `%${magic}\n` : '',
      }],
      defaultInterpreterId,
    } as ZPLNote;

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
