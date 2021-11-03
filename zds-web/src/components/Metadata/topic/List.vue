<template>
  <div class="sa-browse-wrapper">
    <el-breadcrumb separator-class="el-icon-arrow-right">
      <el-breadcrumb-item>
        <span @click="back">Metadata Browse</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item>Topics</el-breadcrumb-item>
    </el-breadcrumb>
    <el-table
      :data="topics"
      border
      max-height="760"
    >
      <el-table-column
        width="280"
        label="Topic Name"
      >
        <template slot-scope="scope">
          <span
            class="topic-name-cell"
            @click="goDetail(scope.row)"
          >{{ scope.row.topic_name }}</span>
        </template>
      </el-table-column>
      <el-table-column
        property="producer_name"
        label="Producer Name"
      />
      <el-table-column
        property="team_dl"
        label="Team DL"
      />
      <el-table-column
        property="description"
        label="Description"
      />
    </el-table>
  </div>
</template>

<script lang="ts">
/* eslint-disable @typescript-eslint/camelcase */
import { Vue, Component } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';

@Component({
  components: {
  },
})
export default class TeamBrowse extends Vue {
  doeRemoteService: DoeRemoteService = DoeRemoteService.instance;

  topics = [];
  
  mounted () {
    this.doeRemoteService.getTopics().then(topics => this.topics = topics);
  }

  back () {
    this.$router.push('/metadata');
  }

  goDetail (row) {
    this.$router.push(`/metadata/topic/${row.topic_name}`);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/metadata.scss';

.sa-browse-wrapper {
  padding: 32px 16px 0;

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

  .el-table {
    margin-top: 32px;
  }
  .el-table__row {
    .topic-name-cell {
      color: #4d8cca;
      cursor: pointer;
      &:hover {
        color: darken(#4d8cca, 20);
      }
    }
    .cell {
      a[href] {
        color: inherit;
        text-decoration: none;
        &:hover {
          color: #4d8cca;
        }
      }
    }
  }
}
</style>
