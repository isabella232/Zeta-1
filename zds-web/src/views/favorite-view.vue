<template>
  <div class="favorite-container">
    <el-tabs>
      <el-tab-pane label="Notebook">
        <span
          slot="label"
          v-click-metric:FAVORITE_TABS_CLICK="{name: 'notebook'}"
        >
          Notebook
        </span>
        <FavoriteNotebook />
      </el-tab-pane>
      <el-tab-pane label="Public Notebook">
        <span
          slot="label"
          v-click-metric:FAVORITE_TABS_CLICK="{name: 'public notebook'}"
        >
          Public Notebook
        </span>
        <FavoritePublicNotebookList />
      </el-tab-pane>
      <el-tab-pane label="Public Notebook">
        <span
          slot="label"
          v-click-metric:FAVORITE_TABS_CLICK="{name: 'shared notebook'}"
        >
          Shared Notebook
        </span>
        <FavoriteShareNotebook />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide  } from 'vue-property-decorator';
import FavoriteNotebook from '@/components/favorite-list/favorite-notebook.vue';
import FavoritePublicNotebookList from '@/components/favorite-list/favorite-public-notebook.vue';
import FavoriteShareNotebook from '@/components/favorite-list/favorite-share-notebook.vue';
import NotebookRemoteService from '../services/remote/NotebookRemoteService';
import ZeppelinApi from '@/services/remote/ZeppelinApi';
@Component({
  components: {
    FavoriteNotebook,
    FavoritePublicNotebookList,
    FavoriteShareNotebook,
  },
})
export default class FavoriteView extends Vue {
  @Provide()
  notebookRemoteService: NotebookRemoteService = new NotebookRemoteService();
  @Provide()
  zeppelinApi: ZeppelinApi = new ZeppelinApi();
  mounted () {
    this.notebookRemoteService.props({
      path: 'favorite',
    });
    this.zeppelinApi.props({
      path: 'favorite',
    });
  }
}
</script>
<style lang="scss" scoped>

</style>
