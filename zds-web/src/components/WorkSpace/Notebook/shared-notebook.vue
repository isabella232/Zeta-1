<script lang="ts">
/**
 * Component <Notebook>. Display content and results of a notebook.
 * Include of <NotebookToolBar>,<Editor> and <ResultWrapper>.
 * Provide actions upon notebook including submit, table check and so on.
 */
import { Component, Prop, Provide, Ref } from 'vue-property-decorator';
import VPisces from '@/components/common/VPisces.vue';
import ResultWrapper from './Result/ResultWrapper.vue';
import Editor from './Editor/Editor.vue';
import EditorToolBar from './Editor/EditorToolBar.vue';
import Notebook from './Notebook.vue';
import { WorkspaceComponentBase, INotebook, IScrollInfo, IEditorHistory, WorkSpaceType, NotebookStatus } from '@/types/workspace';
import SqlConverterRemoteService from '@/services/remote/SQLConverter';
import TableCheckRemoteService from '@/services/remote/SourceTableCheck';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { ZetaExceptionProps } from '@/types/exception';
import DashboardRemoteService from '@/services/remote/Dashboard';
import { wsclient } from '@/net/ws';
import SplitPane from '@/components/common/split-pane/index.vue';
import _ from 'lodash';
import { WorkspaceSrv as NBSrv } from '@/services/Workspace.service';
@Component({
  components: {
    VPisces, ResultWrapper, SplitPane, Editor, EditorToolBar,
  },
})

export default class SharedNotebook extends Notebook {
  @Prop() data: INotebook;
  @Ref('editor') $editor: Editor;
  @Provide()
  notebookRemoteService = new NotebookRemoteService();
  @Provide()
  sqlConverterRemoteService = new SqlConverterRemoteService();
  @Provide()
  tableCheckRemoteService = new TableCheckRemoteService();
  @Provide()
  dashboardRemoteService = new DashboardRemoteService();

  @Provide('secNote')
  @Prop({ default: false })
  secNote: boolean;

  @Provide('viewOnly')
  @Prop({default:true, type:Boolean}) viewOnly: boolean;


  @Provide('runAccess')
  @Prop({default:true, type:Boolean}) runAccess = true;


  reload () {
    if (!this.notebook.loaded) {
      this.notebookRemoteService.getReadOnlyById(this.notebook.notebookId).then((res) => {
        const file = res.data;
        const nb: INotebook = NBSrv.file2nb(file);
        nb.type = WorkSpaceType.SHARED_NOTEBOOK;
        nb.props({
          status_: file.status === 'Connected' ?  NotebookStatus.IDLE : NotebookStatus.OFFLINE,
          type: WorkSpaceType.SHARED_NOTEBOOK,
        });
        if (nb.preference) {
          if (_.keys(nb.preference).indexOf('notebook.layout') >=0 && !nb.preference['notebook.layout']) {
            nb.preference = _.omit(nb.preference, ['notebook.layout']);
          }
        }
        // omit connection here ,because it will init by connection component
        this.$store.dispatch('updateNotebook', _.omit(nb, ['seq', 'status', 'connection', 'jobs']));
        this.loading = false;
        this.onReloaded();
      }).catch(e => {
        this.loading = false;
      });
    }
  }


  /**
     * @deprecated
     */
  scrollInfo: IScrollInfo;
  mounted () {
    const props: ZetaExceptionProps = {
      path: 'notebook',
      workspaceId: this.workspaceId,
    };
    this.sqlConverterRemoteService.props(props);
    this.tableCheckRemoteService.props(props);
    this.notebookRemoteService.props(props);
    this.dashboardRemoteService.props(props);

    // console.log('eventbus:mounted');
    // EventBus.$on('websocket-connected', () => {
    //   this.restoreResult();
    // });
    // if (wsclient.status === Status.LOGGED_IN) {
    //   this.restoreResult();
    // }
  }
  activated () {
    this.reload();
  }
  onSave (code: string) {
    //! disable save event
  }
  onChange ({ history, code }) {
    //! disable change event
  }

  beforeClose () {
    // this.$editor.saveScrollInfo();
  }

  restoreResult () {
  }


}
</script>
