<template>
  <div class="notebook-shared">
    <ReadOnly />
    <template v-if="notebookId">
      <iframe
        v-if="zeppelinUrl"
        ref="zeppelinIFrame"
        :src="zeppelinUrl"
        style="height: 100%;width:100%;border: 0;"
      />
      <ShareLinkDialog
        ref="shareDialog"
        v-model="shareUrl"
        title="Share Notebook"
      />
    </template>
    <template v-else>
      Cannot find Id!
    </template>
  </div>
</template>

<script lang="ts">

import ShareLinkDialog from '@/components/common/share-link-dialog';
import { Component, Vue, Watch, Ref } from 'vue-property-decorator';
import config from '@/config/config';
import ZeppelinMessageHandler from '@/services/zeppelin/zeppelin-message-handler';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import { Getter } from 'vuex-class';
import Util from '@/services/Util.service';
import ReadOnly from '@/components/WorkSpace/Notebook/Editor/Tools/ReadOnly.vue';
import { ZETA_ACTIONS } from '@/components/common/Navigator/nav.config';
const FAV_TYPE = 'share_zpl_nb';
@Component({
  components: {
    ShareLinkDialog,
    ReadOnly
  },
})
export default class ZeppelinNotebookSharedView extends Vue {
  @Getter('getFavoriteById')
  favoriteGetter: (id: string, type: string) => boolean;
  @Ref()
  zeppelinIFrame: HTMLIFrameElement;
  @Ref()
  shareDialog: ShareLinkDialog;

  notebookId = '';
  zplMsgHanlder: ZeppelinMessageHandler = new ZeppelinMessageHandler();
  notebookRemoteService: NotebookRemoteService = new NotebookRemoteService();
  mounted() {
    this.setParams(this.$route.query);
    this.zplMsgHanlder.register()
      .$on('ZPL_GET_NOTE_FVRT', (msg) => {
        const nId = msg.noteId;
        const favorite = this.favoriteGetter(nId, FAV_TYPE);
        const packet = {
          action: 'ZETA_FAVORITE',
          params: {
            favorite,
          }
        };
        this.postMessage(packet);
      })
      .$on('ZPL_SET_NOTE_FVRT', (msg) => {
        if (!msg || !msg.params) {
          return;
        }
        const favorite = msg.params.favorite;
        const nId = msg.noteId;
        // set favorite to Store
        this.notebookRemoteService.favoriteSharedNotebook(nId, favorite, Util.getNt(), FAV_TYPE)
          .then(() => {
            const action = favorite ? 'favorite' : 'unfavorite';
            this.$store.dispatch(action, {
              id: nId,
              favoriteType: FAV_TYPE
            });
          });
      }).$on('ZPL_SHARE_NOTE', (msg) => {
        const nId = msg.noteId;
        if (nId != this.notebookId) {
          return;
        }
        this.shareDialog.open();
      }).$on('ZPL_CLONE_NOTE', (msg) => {
        const nId = msg.noteId;
        const name = msg.params ? msg.params.name : '';
        this.cloneNote(nId, name);
      });
  }

  destroyed() {
    this.zplMsgHanlder.unregister();
  }

  get zeppelinUrl() {
	  const ts = new Date().getTime();
    return `${config.zeta.zeppelin.domain}?zetaIframe&readOnly&nt_login=${Util.getNt()}&t=${ts}#/notebook/${this.notebookId}`;
  }
  get shareUrl() {
    return `${location.protocol}//${location.host}/${Util.getPath()}share/#/notebook/multilang?notebookId=${this.notebookId}`;
  }
  cloneNote(noteId: string, fullname: string) {
    const params = {
      noteId,
      fullname
    };
    const url = `${location.protocol}//${location.host}/${Util.getPath()}#/repository` +`?internal_action=${ZETA_ACTIONS.CLONE_ZPL_NOTE}` +
      `&internal_action_params=${JSON.stringify(params)}`;
    window.open(url, '_blank');
  }
  setParams(query: any){
    this.notebookId = query.notebookId;
  }
  @Watch('$route.query')
  routeQueryChange(newVal: any){
    this.setParams(newVal);
  }
  postMessage(packet: any) {
    this.$nextTick(() => {
      const subWindow = this.zeppelinIFrame.contentWindow;
      if (subWindow) {
        subWindow.postMessage(packet, '*');
      }
    });
  }
}
</script>
<style lang="scss" scoped>
.notebook{
    height: 100%;
}
</style>

