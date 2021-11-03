<template>
  <el-button
    type="text"
    :disabled="loading"
    :icon="getIcon()"
    @click="onFavoriteClick"
  />
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import Util from '@/services/Util.service';
import { FavoriteType } from './favorite-type';
import { FavoriteAction } from './favorite-action';
@Component({
  components: {},
})
export default class FavoriteIconDisplay extends Vue {

  @Inject('notebookRemoteService')
  notebookRemoteService: NotebookRemoteService;
  @Inject()
  favoriteAction: FavoriteAction;

  @Prop() id: string;
  @Prop({ default: 'nb'})
  type: FavoriteType;
  loading = false;
  get favorite() {
    if (!this.favoriteAction) {
      return false;
    }
    return this.favoriteAction.favoriteGetter();
  }

  onFavoriteClick() {
    const currentVal = this.favorite;
    const newVal = !currentVal;
    this.loading = true;
    if (this.type != 'share_nb' && this.type != 'share_zpl_nb' && this.type != 'schedule' && this.type != 'statement') {
      this.notebookRemoteService.favoriteNotebook(this.id, this.type, newVal).then(() => {
        this.favoriteAction.favoriteSetter(newVal);
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    } else {
      this.notebookRemoteService.favoriteSharedNotebook(this.id, newVal, Util.getNt(), this.type).then(() => {
        this.favoriteAction.favoriteSetter(newVal);
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    }

  }
  getIcon() {
    if (this.loading) {
      return 'el-icon-loading';
    }
    if(this.type === 'schedule'){
      return this.favorite ? 'zeta-icon-eye-close' : 'zeta-icon-eye';
    }
    return this.favorite ? 'el-icon-star-on' : 'el-icon-star-off';
  }

}
</script>

