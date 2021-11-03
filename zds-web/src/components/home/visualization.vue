<template>
  <card-template
    :name="name"
    :img-src="'./img/home/visualization.png'"
  >
    <div class="intro-container">
      <div class="image">
        <img :src="'./img/home/visualization_grey.png'">
      </div>
      <span class="intro">
        Explore&nbsp;
        <el-button
          type="text"
          @click="$router.push('repository')"
        >
          Dashboard
        </el-button>&nbsp;and visualize your data.
      </span>
    </div>
    <template slot="quick-route">
      <el-button
        type="text"
        @click="$router.push('repository')"
      >
        Dashboard
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
export default class Visualization extends Vue {
  name = 'Visualization';
}
</script>
