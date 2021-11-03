<template>
  <div class="home">
    <div class="zeta">
      <div class="logo">
        <div class="logo-icon">
          <img :src="'./img/logo.png'">
        </div>

        <div class="logo-name">
          <img :src="'./img/zeta_logo_text.svg'">
        </div>
      </div>
      <div class="desc">
        Unified Data Analytics & Development Platform
      </div>
      <div
        v-if="workspaceAlert"
        class="workspace-alert"
      >
        You have been redirected to homepage due to Zeta currently only allow one active workspace. <br> (You have another active workspace opened in other browser tab.)
      </div>
    </div>
    <div class="guide-card">
      <DataExploration />
      <RunQueries />
      <!-- <Visualization /> -->
      <Collaboration />
      <Scheduling />
    </div>
  </div>
</template>
<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import DataExploration from '@/components/home/data-exploration.vue';
import RunQueries from '@/components/home/run-queries.vue';
import Visualization from '@/components/home/visualization.vue';
import Collaboration from '@/components/home/collaboration.vue';
import Scheduling from '@/components/home/scheduling.vue';
import { Provide } from 'vue-property-decorator';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
@Component({
  components: {
    DataExploration,
    RunQueries,
    Visualization,
    Collaboration,
    Scheduling,
  },
})
export default class HomePage extends Vue {
  @Provide('notebookRemoteService')
  notebookRemoteService = new NotebookRemoteService();
  @Provide('zeppelinApi')
  zeppelinApi = new ZeppelinApi();

  get workspaceAlert() {
    return Boolean(this.$route.query.workspaceAlert);
  }
}
</script>
<style lang="scss" scoped>
.zeta {
  width: 100%;
  margin: 50px auto;
  .logo {
    display: flex;
    justify-content: center;
    margin-bottom: 20px;
    .logo-icon, .logo-name {
      img {
        width: 100%;
      }
    }
    .logo-icon {
      width: 50px;
      margin-right: 20px;
    }
    .logo-name {
      width: 130px;
    }
  }
  .desc {
    font-family: Arial;
    font-style: normal;
    font-weight: normal;
    font-size: 16px;
    line-height: 18px;
    color: #8E8E8E;
    text-align: center;
  }
  .workspace-alert {
    text-align: center;
    color: #999;
    font-family: Arial;
    font-style: normal;
    font-weight: normal;
    font-size: 24px;
  }
}
.guide-card {
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  width: 1200px;
  justify-content: flex-start;
}
</style>
