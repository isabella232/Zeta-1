
<template>
  <card-template
    :name="name"
    :img-src="'./img/home/scheduling.png'"
  >
    <template slot="quick-route">
      <el-button
        type="text"
        @click="$router.push('schedule')"
      >
        Scheduler
      </el-button>
    </template>
    <div class="intro-container">
      <div class="image">
        <img :src="'./img/home/scheduling_grey.png'">
      </div>
      <span class="intro">
        Set up new&nbsp;
        <el-button
          type="text"
          @click="$router.push('schedule')"
        >
          Scheduler
        </el-button>&nbsp;
        to automate your queries.
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
export default class Scheduling extends Vue {
  name = 'Scheduling';
  routeConfig = [{
    name: 'Scheduler',
    path: 'schedule',
  }];
}
</script>
