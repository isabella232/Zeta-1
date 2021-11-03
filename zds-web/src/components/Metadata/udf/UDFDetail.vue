<template>
  <div class="udf-detail-wrapper">
    <el-breadcrumb separator-class="el-icon-arrow-right">
      <el-breadcrumb-item>
        <span @click="backToMain">Metadata Browse</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item>
        <span @click="backToList">UDF List</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item>UDF Detail</el-breadcrumb-item>
    </el-breadcrumb>

    <h1>{{ udfName }}</h1>

    <div class="tabbar">
      <ul class="tab">
        <li class="active">
          Overview
        </li>
      </ul>
      <div class="btn-group">
        <el-button
          v-if="isEditing"
          @click="cancel"
        >
          Cancel
        </el-button>
        <el-button
          v-if="isEditing"
          type="primary"
          @click="save"
        >
          Save
        </el-button>
        <el-button
          v-else
          type="primary"
          @click="edit"
        >
          Edit
        </el-button>
        <!-- <el-button
          v-if="isOwner && !isEditing"
          type="danger"
          @click="deleteUdf"
        >
          Delete
        </el-button> -->
      </div>
    </div>
    <div
      v-if="udfData"
      class="detail-content"
    >
      <div class="detail-row">
        <label>Database</label>
        <span>{{ udfData.db_name }}</span>
      </div>

      <div class="detail-row">
        <label>Owner</label>
        <user-selection
          :disabled="!isEditing"
          :nt="form.owner"
          @change="val => form.owner = val.nt || ''"
        />
      </div>
      <div class="detail-row">
        <label>Team DL</label>
        <template v-if="isEditing">
          <el-input
            v-model="form.team_dl"
            placeholder="Enter email address"
          />
        </template>
        <span v-else>{{ udfData.team_dl }}</span>
      </div>
      <div class="detail-row">
        <label>Platform</label>
        <span>
          <i
            v-for="p in platforms"
            :key="p"
            :title="p"
            :class="p.toLowerCase()"
            class="platform-icon"
          />
        </span>
      </div>
      <div class="detail-row">
        <label>Description</label>
        <quill-editor
          v-if="isEditing"
          v-model="form.description"
          :options="editorOpt"
        />

        <span
          v-else
          v-html="udfData.description"
        />
      </div>

      <div class="detail-row">
        <label>Class Name</label>
        <span>{{ udfData.class_name }}</span>
      </div>
      <div class="detail-row">
        <label>Parameters</label>
        <el-input
          v-if="isEditing"
          v-model="form.parameters"
          placeholder="Enter UDF parameters"
        />
        <span v-else>{{ udfData.parameters }}</span>
      </div>
      <div class="detail-row">
        <label>Example</label>
        <quill-editor
          v-if="isEditing"
          v-model="form.example"
          :options="editorOpt"
        />
        <span
          v-else
          v-html="udfData.example"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
/* eslint-disable @typescript-eslint/camelcase */
import { Vue, Component, Watch } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import MetadataService from '@/services/remote/Metadata';
import { quillEditor } from 'vue-quill-editor';
import { EditableUDFMetadata } from '../components/types';
import UserSelection from '@/components/common/UserSelection.vue';

import _ from 'lodash';
import { attempt } from '@drewxiu/utils';
import Util from '../../../services/Util.service';

export const EditorOptions = {
  modules: {
    toolbar: [
      [{ size: ['small', false, 'large'] }],
      ['bold', 'italic'],
      [{ list: 'ordered' }, { list: 'bullet' }],
      ['link', 'image'],
    ],
  },
};

@Component({
  components: {
    quillEditor,
    UserSelection,
  },
})
export default class UDFDetail extends Vue {
  doeRemoteService: DoeRemoteService = DoeRemoteService.instance;
  metadataService: MetadataService = new MetadataService();
  udfName: string;
  isEditing = false;
  editorOpt = EditorOptions;
  userOptions: any[] = [];

  form: EditableUDFMetadata = {
    owner: '',
    description: '',
    parameters: '',
    example: '',
    team_dl: '',
  };

  get isOwner () {
    return attempt(() => this.udfData.owner === Util.getNt());
  }

  created () {
    const { $route, $store } = this;
    let [db, name]= $route.params.name.split('.');
    if (!name) {
      name = db;
      db = 'default';
    }
    if (!name) {
      this.backToList();
    } else {
      this.udfName = name;
      $store.dispatch('getUdfByName', { name, db });
    }
  }

  get udfData () {
    return this.$store.state.Metadata.udfData;
  }

  get platforms () {
    return this.udfData.platform.split(',');
  }

  get isValidTeamDL () {
    const { team_dl } = this.form;
    if (team_dl) {
      return /^([a-zA-Z0-9_\-\.]+)@([a-zA-Z0-9_\-\.]+)\.([a-zA-Z]{2,5})$/.test(team_dl);
    }
    return true;
  }

  @Watch('udfData')
  updateForm () {
    this.reset();
  }
  
  backToMain () {
    this.$router.push('/metadata');
  }

  backToList () {
    this.$router.push('/metadata/udf');
  }

  edit () {
    this.isEditing = true;
  }

  deleteUdf () {
    // todo, want this?
  }

  async save () {
    const { udfData, isValidTeamDL, form, udfName, doeRemoteService, metadataService, $store, $message } = this;
    if (!isValidTeamDL) {
      $message.error('Invalid Team DL');
      return;
    }
    await doeRemoteService.updateUDF(form);
    metadataService.createESUDF(udfData);
    this.isEditing = false;
    $store.dispatch('getUdfByName', { name: udfName, db: udfData.db_name });
  }

  cancel () {
    this.isEditing = false;
    this.reset();
  }

  reset () {
    if (this.udfData) {
      this.form = { ...this.udfData };
    }
  }

  userSearchHandler (queryStr: string) {
    this.doeRemoteService.getUser(queryStr).then((res) => {
      const userOptions: Array<any> = [];
      if (res && res.data && res.data.response && res.data.response.docs) {
        _.forEach(res.data.response.docs, (v: any) => {
          const option: any = {
            nt: v.nt,
            value:
              (v.last_name ? v.last_name : '') +
              (v.last_name && v.preferred_name ? ',' : '') +
              (v.preferred_name ? v.preferred_name : '') +
              '(' +
              v.nt +
              ')',
          };
          userOptions.push(option);
        });
      }

      this.userOptions = userOptions;
    });
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
@import '@/styles/metadata.scss';

.udf-detail-wrapper {
  overflow-y: auto;
  height: calc(100vh - 102px);
  padding: 32px 16px;

  .el-select, .el-input {
    vertical-align: middle;
  }
  .el-breadcrumb__item {
    &:not(:last-of-type) {
      cursor: pointer;
      span {
        color: #4d8cca;
        &:hover {
          text-decoration: underline;
        }
      }
    }
  }

  h1 {
    margin: 24px 0 12px;
    font-size: 18px;
    font-style: normal;
    font-weight: 700;
  }

  .tabbar {
    display: flex;
    width: 100%;
    border-bottom: 1px solid #cacbcf;

    ul.tab {
      display: flex;
      flex: 1;
      height: 100%;
      list-style-type: none;
      > li {
        align-items: center;
        border-bottom: 1px solid #cacbcf;
        color: #999;
        cursor: pointer;
        display: flex;
        font-size: 14px;
        padding: 10px 20px;
        position: relative;
        bottom: -1px;
        &.active {
          border-bottom: 1px solid #569ce1;
          background-color: #fff;
          color: $zeta-global-color;
        }
        &:hover {
          color: #569ce1;
        }
      }
    }
  }
  .detail-content {
    padding: 20px 0;

    :global(.ql-container) {
      min-height: 120px;
    }

    .detail-row {
      padding: 15px 0;
      label {
        display: inline-block;
        width: 150px;
        font-weight: bold;
      }

      .platform-icon {
        padding: 4px 8px;
        font-size: 14px;
      }
      > span,
      .quill-editor {
        width: 80%;
        display: inline-block;
        vertical-align: text-top;
        word-break: break-word;
      }
      .el-input {
        max-width: 40%;
      }
    }
  }
}
</style>
