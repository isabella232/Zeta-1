<template>
  <div class="datamove-schedule">
    <h3 class="schedule-title">Data Move Details</h3>
    <div class="dataMove-info">
      <div class="row">
        <div class="row-name">Source</div>
        <div class="row-info">
          <span class="disable">{{ task.history.sourcePlatform === 'numozart'?'mozart':task.history.sourcePlatform }}</span>
        </div>
      </div>
      <div class="row">
        <div class="row-name">Source table</div>
        <div class="row-info">
          <span class="disable">{{ task.history.sourceTable }}</span>
        </div>
      </div>
      <div class="row row-3" :class="task.history.type != 4?'':'hidden'">
        <div class="row-name">Filters</div>
        <div class="row-info">
          <span class="disable">{{ task.filter }}</span>
        </div>
      </div>
      <div class="row">
        <div class="row-name">Target</div>
        <div class="row-info">
          <span class="disable">{{ task.history.targetPlatform }}</span>
        </div>
      </div>
      <div class="row" v-if="task.history.type != 4">
        <div class="row-name">Hercules table</div>
        <div class="row-info">
          <span class="disable">{{ task.history.targetTable }}</span>
        </div>
      </div>
      <div class="row" v-if="task.history.type == 4">
        <div class="row-name">Target database</div>
        <div class="row-info">
          <span class="disable">{{ task.history.targetTable }}</span>
        </div>
      </div>
      <div class="row">
        <div class="row-name"></div>
        <div class="row-info">
          <el-checkbox :disabled="true" :value="Boolean(task.isDrop)" />
          <span class="override">Override the exisitng table</span>
        </div>
      </div>
      <div class="row" v-if="showWarning()">
        <div class="row-name"></div>
        <div class="row-info">
          <div class="alert">Warning – Please be aware the overrided data can no longer be recovered.</div>
        </div>
      </div>
      <div class="row">
        <div class="row-name"></div>
        <div class="row-info">
          <el-checkbox :disabled="true" :value="Boolean(task.isConvert)" />
          <span class="convert">Column Type Not Change</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import {
  DataMoveSchedule,
} from '@/components/common/schedule-container';

@Component({
  components: {}
})
export default class DataMoveScheduleInfo extends Vue {
  // @Prop() type:ScheduleType
  @Prop() task: DataMoveSchedule;
  constructor() {
    super();
  }
  showWarning(){
    if(this.task && this.task.history.type === 4 && this.task.history.sourcePlatform === 'numozart'){
      return true;
    }
    return false;
  }
}
</script>
<style lang="scss" scoped>
.datamove-schedule {
  .override,
  .convert {
    margin-left: 10px;
  }
  .alert{
    color: #F3AF2B
  }
  .hidden{
    display: none;
  }
  .disable{
    background-color: #F5F7FA;
    border-color: #E4E7ED;
    color: #C0C4CC;
    cursor: not-allowed;
    line-height: 28px;
    height: 30px;
    background-image: none;
    border-radius: 4px;
    border: 1px solid #DCDFE6;
    display: inline-block;
    box-sizing: border-box;
    padding: 0 15px;
    width: 100%;
    outline: none;
  }
}
.schedule-detail-page{
  .dataMove-info{
    display: grid;
    grid-template-columns: repeat(2, calc(50% - 20px));
    grid-template-rows: repeat(5, auto);
    grid-column-gap: 20px;
    grid-auto-flow: column;
  }
  .row-3{
    grid-column: 1 / 2;
    grid-row: 3 / 6;
  }
  .hidden{
    visibility: hidden !important;
  }
}
</style>
