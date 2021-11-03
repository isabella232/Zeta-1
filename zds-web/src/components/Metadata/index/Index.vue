<template>
  <div class="metadata-index">
    <Header 
      :show-back-button="false"
      :breadcrumb="[{ key: 'Metadata', label: 'Metadata' }]"
    />
    <el-row
      :gutter="20"
      class="main"
    >
      <el-col
        :span="18"
        class="cards"
      >
        <span style="font-size: 18px;font-weight: bold; color: black;">
          Domain
        </span>
        <div class="card-grid">
          <DomainCard
            v-for="d in domains"
            :key="d.domain"
            :title="d.domain"
            :tables="d.cnt"
            :users="d.user_cnt"
            :batches="d.batch_cnt"
            :actions="getDomainActions(d)"
          />
        </div>
      </el-col>
      <el-col
        :span="6"
        class="recent-annoucement"
      >
        <recent-annoucement />
      </el-col>
    </el-row>
  </div>
</template>
<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import Header from './components/Header.vue';
import DomainCard from './components/DomainCard/index.vue';
import RecentAnnoucement from './components/RecentAnnoucement.vue';
import MetadataIndex, { Actions } from './store';
import { Action, State } from 'vuex-class';
import { isEmpty } from '@drewxiu/utils';

@Component({
  components: {
    Header,
    DomainCard,
    RecentAnnoucement,
  },
})
export default class Metadata extends Vue {

  @Action(Actions.GetDomainList) getDomainList;
  @State(state => state.MetadataIndex.domains) domains;

  created () {
    if (!this.$store.state[MetadataIndex.namespace]) {
      this.$store.registerModule(MetadataIndex.namespace, MetadataIndex);
    }
    if (isEmpty(this.domains)) {
      this.getDomainList();
    }
  }
  browseBy (cmd) {
    this.$router.push(`/metadata/${cmd}`);
  }
  getDomainActions (domain) {
    const path = '/metadata/datasets';
    const sub = domain.sub_domain || [];
    return [
      { label: 'All', action: () => this.$router.push(`${path}?domain=${domain.domain}`) },
      ...sub.map(label => ({ label, action: () => this.$router.push(`${path}?domain=${domain.domain}&subdomain=${label}`) })),
    ];
  }
}
</script>
<style lang="scss" scoped>

.metadata-index {
  ::v-deep {
    .el-breadcrumb__inner {
      font-size: 18px;
      font-weight: bold;
    }
  }
  .main {
    padding: 24px 12px;
    .dropdown {
      label {
        position: absolute;
        font-size: 12px;
        top: -16px;
        font-weight: normal;
        visibility: hidden;
      }
      &:hover label {
        visibility: visible;
      }
    }
    .card-grid {
      padding-top: 18px;
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      column-gap: 24px;
      row-gap: 24px;
    }
  }
}
</style>
<style lang="scss">
.browse-by {
  margin-left: 100px;
  margin-top: -10px;
  color: green;
}
</style>