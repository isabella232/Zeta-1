<template>
  <div class="table-status-tabdiv">
    <line-chart title="Job Delay" :collectionData="collectionData"/>
  </div>
</template>

<script lang="ts">
/* eslint-disable @typescript-eslint/camelcase */
import { Vue, Component, Prop } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import LineChart from './components/LineChart.vue';

@Component({
  components: {
    LineChart,
  }
})
export default class TableStatus extends Vue {
  @Prop() collectionData: any;
  doeRemoteService: DoeRemoteService = DoeRemoteService.instance;

}
</script>
<style lang="scss" scoped>
@import '@/styles/metadata.scss';

.table-status-tabdiv {
  height: 100%;
}
</style>
