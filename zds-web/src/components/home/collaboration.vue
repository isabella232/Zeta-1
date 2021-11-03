
<template>
  <card-template
    :name="name"
    :img-src="'./img/home/collaboration.png'"
  >
    <template slot="quick-route">
      <el-button
        type="text"
        @click="$router.push('favorite')"
      >
        Favorite
      </el-button>
      <span class="splitter" />
      <el-button
        type="text"
        @click="$router.push('zeta-sheet')"
      >
        ZETA Sheet
      </el-button>
    </template>
    <div class="intro-container">
      <div class="image">
        <img :src="'./img/home/collaboration_grey.png'">
      </div>
      <span class="intro">
        Add anything to your&nbsp;<el-button
          type="text"
          @click="$router.push('favorite')"
        >
          Favorite
        </el-button>.
      </span>
    </div>
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
export default class Collaboration extends Vue {
  name = 'Collaboration';
  routeConfig = [{
    name: 'Favorite',
    path: 'favorite',
  },{
    name: 'ZETA Sheet',
    path: 'zeta-sheet',
  }];
}
</script>
