<template>
  <favorite-icon 
    :id="notebookId" 
    v-click-metric:NB_TOOLBAR_CLICK="{name: 'favorite'}"
    v-bind="$attrs"
  />
</template>

<script lang="ts">
import { Component, Vue, Prop, } from 'vue-property-decorator';
import FavoriteIcon from '@/components/common/favorite-icon/favorite-icon-witn-store.vue';

@Component({
  components: {
    FavoriteIcon,
  },
  inheritAttrs: false,
})
export default class Favorite extends Vue {
  @Prop() notebookId: string;
}
</script>

<style lang="scss" scoped>
* /deep/ {
  .el-icon-star-off,
  .el-icon-star-on {
    font-size: 18px;
    margin-top: -4px;
  }
}
</style>
