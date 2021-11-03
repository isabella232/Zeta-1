<template>
  <div
    v-loading="loading"
    class="notebook-shared"
  >
    <template v-if="notebookId">
      <Notebook
        v-if="notebook"
        class="notebook"
        :data="notebook"
        view-only
        v-bind="$attrs"
      />
    </template>
    <template v-else>
      Cannot find Id!
    </template>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import { INotebook } from '@/types/workspace';
import { WorkspaceSrv as WSSrv } from '@/services/Workspace.service';
import Notebook from '@/components/WorkSpace/Notebook/Notebook.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
@Component({
  components: {
    Notebook,
  },
})
export default class NotebookSharedView extends Vue {
  notebookRemoteService = new NotebookRemoteService().props({path: 'notebook'});
  notebookId = '';
  notebook: INotebook | undefined = undefined;
  loading = false;
  mounted () {
    this.setParams(this.$route.query);
  }
  setParams (query: any){
    this.notebookId = query.notebookId;
    this.load(this.notebookId);
  }
  @Watch('$route.query')
  routeQueryChange (newVal: any){
    this.setParams(newVal);
  }
  load (id: string){
    this.loading = true;
    this.notebookRemoteService.getSharedNotebook(id).then(res => {
      const file = res.data;
      this.notebook = WSSrv.file2nb(file);
      this.loading = false;
      // Vue.prototype.$shareName = this.notebook.name
      this.$breadcrumbs.addSubPath(this.notebook.name);
    }).catch((err)=>{
      console.log(err);
      this.loading = false;
      this.$message.warning('Not found notebook!');
    });

  }
}
</script>
<style lang="scss" scoped>
.notebook{
    height: 100%;
}
</style>

