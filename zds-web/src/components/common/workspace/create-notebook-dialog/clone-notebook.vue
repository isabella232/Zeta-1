<template>
  <div v-loading="cloneLoading">
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
import { Component, Prop, Emit, Ref } from 'vue-property-decorator';
import { NoteForm } from './create-notebook-form/note-form';
import { RestPacket } from '@/types/RestPacket';
import { IPreference, CodeType } from '@/types/workspace';

import Util from '@/services/Util.service';
import { ZetaException } from '@/types/exception';
import NotebookForm from './create-notebook-form/notebook-form.vue';
import CreateNotebook from './create-notebook.vue';
import PlatformPermissionProvider from './platform-permission-provider';
import FolderOptionsProvider from './folder-options-provider';
import { parseName } from '@/services/zeppelin/Zeppelin.service';
import _ from 'lodash';
@Component({
  components: {
    NotebookForm,
  },
  mixins: [
    PlatformPermissionProvider,
    FolderOptionsProvider,
  ],
})
export default class CloneNotebook extends CreateNotebook {
  @Ref('notebookForm')
  $noteForm: NotebookForm;
  @Prop()
  cloneId: string;
  @Prop()
  clone: 'zeta' | 'zeppelin';

  cloneLoading = false;
  cloneExtraInfo = {
    context: '',
    preference: {} as IPreference,
    srcId: '',
  };


  mounted() {
    if (this.cloneId && this.clone) {
      this.loadNoteInfo(this.cloneId, this.clone);
    }
  }
  loadNoteInfo(id: string, type: 'zeta' | 'zeppelin') {
    if (type === 'zeta') {
      this.getNotebookInfo(id);
    } else {
      this.getZeppelinNotebookInfo(id);
    }
  }

  @Emit('new-notebook-success')
  createSucceed() {
    this.$message({
      type: 'success',
      message: 'Successfully clone notebook on server.',
      customClass: 'crt-success-message',
    });
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
    this.createLoading = true;
    let request;
    if (noteForm.noteType === 'stacked') {
      request = this.cloneZeppelinNote(this.cloneExtraInfo.srcId,noteForm);
    } else {
      request = this.cloneZetaNotebook(noteForm);
    }
    request.then((id: string) => {
      this.addFile(id, noteForm);
      this.addWorkspace(id, noteForm);
      this.createSucceed();
    }).catch((e: any) => {
      this.createLoading = false;
      // this.createFailed(e);
    });
  }

  getNotebookInfo(notebookId: string) {
    this.form.noteType = 'solo';
    this.cloneLoading = true;
    this.notebookRemoteService.getCloneById(notebookId).then(res => {
      const file = res.data as RestPacket.File;
      this.form.folder = file.path;
      this.form.name = this.getCloneName(file.title);
      this.cloneExtraInfo.context = file.content;
      this.cloneExtraInfo.preference = JSON.parse(file.preference);
      this.setNoteFormByPreference(this.form, this.cloneExtraInfo.preference);
    }).catch(err => {
      console.error('get Clone NoteBook error: ' + err);
      err.message = 'Fail to get clone notebook';
    }).finally(() => {
      this.cloneLoading = false;
    });
  }

  getZeppelinNotebookInfo(notebookId: string) {
    this.form.noteType = 'stacked';
    this.cloneLoading = true;
    this.zeppelinApi.getNotesBrief(notebookId).then(res => {
      const noteData = res.data.body?res.data.body[0]:null;
      if(!noteData) throw new Error('cannot find notebook ' + notebookId);
      const {path, name} = parseName(noteData.name);
      this.form.folder = path;
      this.form.name = this.getCloneName(name);
      this.setNoteFormByInterpreter(this.form, noteData.interpreterSettings);
      this.cloneExtraInfo.srcId = notebookId;
    }).catch(err => {
      console.error('get Clone NoteBook error: ' + err);
      err.message = 'Fail to get clone notebook';
    }).finally(() => {
      this.cloneLoading = false;
    });
  }

  cloneZeppelinNote(notebookId: string, noteForm: NoteForm) {
    this.createLoading = true;
    const fullname = noteForm.folder + noteForm.name;
    return this.zeppelinApi.clone(notebookId, { name: fullname }).then((res) => {
      const notebookId = res.data.body;
      this.createLoading = false;
      return notebookId;
    }).catch((e) => {
      if(e.originalResponse) {
        const response = e.originalResponse;
        const isDuplicateName = response.data.status === 'EXPECTATION_FAILED' && response.data.message === 'duplicate file name';
        if(isDuplicateName) {
          e.resolve();
          this.form.nameError = response.data.message;
        }
      }
      this.createLoading = false;
    });
  }

  cloneZetaNotebook(noteForm: NoteForm, seq = -1, opened = -1): Promise<any> {
    if (this.cloneExtraInfo.preference && this.cloneExtraInfo.preference['notebook.connection']) {
      this.cloneExtraInfo.preference['notebook.connection'].batchAccount = '';
    }
    const packet: RestPacket.File = {
      id: uuid(),
      nt: Util.getNt(),
      content: this.cloneExtraInfo.context,
      path: this.parseFolder(noteForm.folder),
      title: noteForm.name,
      createDt: moment().valueOf(),
      updateDt: moment().valueOf(),
      preference: JSON.stringify(this.cloneExtraInfo.preference),
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
          this.form.nameError = e.message;
        }
        throw e;
      });
  }
  getCloneName(name: string) {
    return `${name}_Clone`;
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
  setNoteFormByInterpreter(noteForm: NoteForm, interpreterSettings: any[]) {
    const interpreter = _.chain(interpreterSettings)
      .filter(setting => setting.selected)
      .filter(setting => setting.id != 'md' && setting.id != 'angular')
      .find()
      .value();
    if (!interpreter) {
      return;
    }
    if (interpreter.id === 'hermes') {
      noteForm.interpreter = 'Spark SQL';
      noteForm.platform = 'hermes';
    } else if (interpreter.id === 'kylin') {
      noteForm.interpreter = 'Kylin';
      noteForm.platform = 'kylin';
    } else {
      noteForm.interpreter = 'Spark SQL';
      noteForm.platform = 'apollo';
    }
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
