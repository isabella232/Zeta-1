
<template>
  <card-template
    :name="name"
    :img-src="'./img/home/operation.png'"
  >
    <div class="intro-container">
      <div class="image">
        <img :src="'./img/home/operation_grey.png'">
      </div>
      <span class="intro">
        Start your analytics journey and explore&nbsp;
        <el-button
          type="text"
          @click="$router.push('metadatabrowse')"
        >
          Metadata
        </el-button>&nbsp;here.
      </span>
    </div>
    <template slot="quick-route">
      <el-button
        type="text"
        @click="$router.push('metadatabrowse')"
      >
        Metadata
      </el-button>
    </template>
  </card-template>
</template>

<script lang="ts">

import { Component, Vue } from 'vue-property-decorator';
import CardTemplate from '@/components/home/card-template.vue';
@Component({
  components: {
    CardTemplate,
  },
})
export default class Operation extends Vue {
  name = 'Operation';
  routeConfig = [{
    name: 'Favorite',
    path: 'favorite',
  },{
    name: 'ZETA Sheet',
    path: 'zeta-sheet',
  }];
}
</script>
