<template>
  <FavoriteIconDisplay
    :id="id"
    :type="type"
  />
</template>

<script lang="ts">
import { Component, Vue, Prop, Provide, Emit, } from 'vue-property-decorator';
import { Getter } from 'vuex-class';
import { FavoriteType } from './favorite-type';
import { FavoriteAction } from './favorite-action';

import FavoriteIconDisplay from '@/components/common/favorite-icon/favorite-icon-display.vue';
@Component({
  components: {
    FavoriteIconDisplay,
  }
})
export default class FavoriteIcon extends Vue {
  @Prop() id: string;
  @Prop({ default: 'nb'})
  type: FavoriteType;
  @Prop()
  value: boolean;

  @Emit('input')
  // eslint-disable-next-line @typescript-eslint/no-unused-vars, @typescript-eslint/no-empty-function
  emitValue(val: boolean) {

  }

  @Getter('getFavoriteById')
  favoriteGetter: (id: string, type: FavoriteType) => boolean;
  loading = false;
  
  @Provide()
  favoriteAction: FavoriteAction = {
  } as FavoriteAction;
  
  created() {
    this.favoriteAction.favoriteGetter = () => {
      return this.value;
    };
    this.favoriteAction.favoriteSetter = (val: any) =>  {
      this.emitValue(val);
    };
  }
}
</script>

