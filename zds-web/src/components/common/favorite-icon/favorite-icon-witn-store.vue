<template>
  <FavoriteIconDisplay
    :id="id"
    :type="type"
  />
</template>

<script lang="ts">
import { Component, Vue, Prop, Provide } from 'vue-property-decorator';
import { Getter } from 'vuex-class';
import { FavoriteType } from './favorite-type';
import { FavoriteAction } from './favorite-action';

import FavoriteIconDisplay from '@/components/common/favorite-icon/favorite-icon-display.vue';
@Component({
  components: {
    FavoriteIconDisplay,
  },
})
export default class FavoriteIcon extends Vue {
  @Prop() id: string;
  @Prop({ default: 'nb'})
  type: FavoriteType;

  @Getter('getFavoriteById')
  favoriteGetter: (id: string, type: FavoriteType) => boolean;
  loading = false;

  @Provide()
  favoriteAction: FavoriteAction = {
  } as FavoriteAction;

  created() {
    this.favoriteAction.favoriteGetter = () => {
      return this.favoriteGetter(this.id + '', this.type);
    };
    this.favoriteAction.favoriteSetter=  (val: any) => {
      const action = val ? 'favorite' : 'unfavorite';
      this.$store.dispatch(action, {
        id: this.id + '',
        favoriteType: this.type,
      });
    };
  }
}
</script>

