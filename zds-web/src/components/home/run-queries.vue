<template>
  <div>
    <card-template
      :name="name"
      :img-src="'./img/home/run-queries.png'"
    >
      <template slot="quick-route">
        <el-button
          type="text"
          @click="$router.push('notebook')"
        >
          Workspace
        </el-button>
        <span class="splitter" />
        <el-button
          type="text"
          @click="$router.push('repository')"
        >
          Repository
        </el-button>
      </template>
      <div class="intro-container">
        <div class="image">
          <img :src="'./img/home/run-queries_grey.png'">
        </div>
        <span class="intro">
          Create new &nbsp;
          <el-button
            type="text"
            @click="createNBVis = true"
          >
            Notebook
          </el-button>&nbsp; and run queries to get insights.

        </span>
      </div>
    </card-template>
    <new-notebook-dialog
      :visible.sync="createNBVis"
    />
  </div>
</template>

<script lang="ts">

import { Component, Vue } from 'vue-property-decorator';
import CardTemplate from '@/components/home/card-template.vue';
import NewNotebookDialog from '@/components/common/workspace/create-notebook-dialog/create-notebook-dialog.vue';
@Component({
  components: {
    CardTemplate,
    NewNotebookDialog,
  },
})
export default class RunQueries extends Vue {
  name = 'Run Queries';
  createNBVis = false;
}
</script>
