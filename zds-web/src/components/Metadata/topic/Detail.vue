<template>
  <div class="team-detail-wrapper">
    <el-breadcrumb separator-class="el-icon-arrow-right">
      <el-breadcrumb-item>
        <span @click="backToMain">Metadata Browse</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item>
        <span @click="backToList">Topics</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item>Detail</el-breadcrumb-item>
    </el-breadcrumb>

    <h1>{{ id }}</h1>

    <div class="tabbar">
      <ul class="tab">
        <li
          :class="activeTab === 'overview' && 'active'"
          @click="activeTab = 'overview'"
        >
          Overview
        </li>
        <li
          :class="activeTab === 'fields' && 'active'"
          @click="activeTab = 'fields'"
        >
          Fields
        </li>
      </ul>
    </div>
    <div
      v-if="activeTab === 'overview' && topic"
      class="detail-content"
    >
      <div class="detail-row">
        <label>Description</label>
        <span>{{ topic.description }}</span>
      </div>
      <div class="detail-row">
        <label>Owner</label>
        <div class="user-group">
          <user-selection
            v-for="nt in teamContacts"
            :key="nt"
            :nt="nt"
          />
        </div>
      </div>
      <div class="detail-row">
        <label>Team DL</label>
        <a
          :href="'mailto:'+ topic.team_dl"
        >{{ topic.team_dl }}</a>
      </div>
      <div class="detail-row">
        <label>Schema</label>
        <a
          style="cursor:pointer;"
          @click="() => showSchemaJson = true"
        >{{ topic.subject }}</a>
      </div>
      <div class="detail-row">
        <label>Producer</label>
        <span>{{ topic.producer_name }}</span>
      </div>
      <div class="detail-row">
        <label>Consumer</label>
        <span>
          <el-tag
            v-for="c in consumers"
            :key="c"
          >
            {{ c }}
          </el-tag>
        </span>
      </div>

      <div class="separator" />
      <div class="detail-row">
        <label>Partitions</label>
        <span>{{ topic.partitions }}</span>
      </div>
      <div class="detail-row">
        <label>Replicas</label>
        <span>{{ topic.replicas }}</span>
      </div>
      <div class="detail-row">
        <label>Retention</label>
        <span>{{ topic.retention_minutes }} Minutes</span>
      </div>
    </div>
    <fields
      v-if="activeTab === 'fields' && topic"
      :topic="id"
    />
    <el-dialog
      :visible.sync="showSchemaJson"
      width="50%"
      top="8vh"
      lock-scroll
    >
      <template slot="title">
        <div class="dialog-title">
          Schema of <strong>{{ id }}</strong>
        </div>
      </template>
      <div class="json-wrapper">
        <json-highlight :json="schema" />
      </div>
      <footer slot="footer">
        <el-button
          type="primary"
          @click="() => copy2Clipboard(schema)"
        >
          Copy
        </el-button>
        <el-button
          @click="showSchemaJson = false"
        >
          Close
        </el-button>
      </footer>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Vue, Component } from 'vue-property-decorator';
import UserSelection from '@/components/common/UserSelection.vue';
import Fields from './components/Fields.vue';
import JsonHighlight from '@/components/common/JsonHighlight.vue';
import SqlHighlight from '@/components/common/SqlHighlight.vue';
import DoeRemoteService from '../../../services/remote/DoeRemoteService';
import { attempt } from '@drewxiu/utils';
import { copy2Clipboard } from '../utils';

const api = DoeRemoteService.instance;

@Component({
  components: {
    UserSelection,
    Fields,
    JsonHighlight,
    SqlHighlight,
  },
})
export default class TopicDetail extends Vue {

  id: string;
  topic: any = {};
  activeTab: 'overview' | 'fields' = 'overview';
  showSchemaJson = false;

  created() {
    const { $route } = this;
    this.id= $route.params.id;
    api.getTopicByName(this.id).then(resp => this.topic = resp);
  }

  get teamContacts() {
    return attempt(() => this.topic.team_contacts.split(',').filter(Boolean), []);
  }

  get consumers() {
    return attempt(() => this.topic.consumer.split(',').filter(Boolean), []);
  }

  get schema() {
    return attempt(() => JSON.parse(this.topic.schema), {});
  }

  async copy2Clipboard(str: string) {
    await copy2Clipboard(str);
    this.$message.success('Successfully copied');
  }

  backToMain() {
    this.$router.push('/metadata');
  }

  backToList() {
    this.$router.push('/metadata/topic');
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
@import '@/styles/metadata.scss';

.team-detail-wrapper {
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

    .separator {
      border-bottom: 1px solid #f2f2f2;
      height: 1px;
      margin: 12px 0 ;
    }

    .detail-row {
      padding: 15px 0;
      label {
        display: inline-block;
        width: 150px;
        font-weight: bold;
      }
      .el-tag + .el-tag {
        margin-left: 8px;
      }
      .platform-icon {
        padding: 4px 8px;
        font-size: 14px;
      }
      > span {
        width: 80%;
        display: inline-block;
        vertical-align: text-top;
        word-break: break-word;
      }
      .user-group {
        display: inline-block;
        > * {
          margin-right: 24px;
        }
      }
    }
  }
  .dialog-title {
    padding-bottom: 10px;
    > span {
      margin-left: 12px;
      cursor: pointer;
    }
  }
  .sql-wrapper,
  .json-wrapper {
    max-height: 75vh;
    overflow: scroll;
  }
  .ddl {
    max-height: 60px;
    overflow: hidden;
    font-size: .8em;
    cursor: pointer;
    &:hover {
      color: #569ce1 !important;
      :global(span) {
        color: #569ce1 !important;
      }
    }
  }
}

</style>
