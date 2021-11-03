<template>
  <div class="dataMove-schedule">
    <h3 class="schedule-title">Data Validation Details</h3>
    <div class="dataMove-info">
      <div class="row">
        <div class="row-name">
          Source
        </div>
        <div class="row-info">
          <span class="disable">{{ task.history.sourcePlatform ==='mozart_lvs'?'mozart':task.history.sourcePlatform }}</span>
        </div>
      </div>
      <div class="row">
        <div class="row-name">
          Source Table
        </div>
        <div class="row-info">
          <span class="disable">{{ task.history.sourceTable }}</span>
        </div>
      </div>
      <div class="row">
        <div class="row-name">
          ETL_ID or DW_SA
        </div>
        <div class="row-info">
          <span class="disable">{{ task.sourceBatchAccount }}</span>
        </div>
      </div>
      <div class="row">
        <div class="row-name">
          Source Filters
        </div>
        <div class="row-info">
          <span class="disable">{{ task.sourceFilter }}</span>
        </div>
      </div>
      <div class="row">
        <div class="row-name">
          Destination
        </div>
        <div class="row-info">
          <span class="disable">{{ task.history.targetPlatform ==='mozart_lvs'?'numozart':task.history.targetPlatform }}</span>
        </div>
      </div>
      <div class="row">
        <div class="row-name">
          Target Table
        </div>
        <div class="row-info">
          <span class="disable">{{ task.history.targetTable }}</span>
        </div>
      </div>
      <div class="row">
        <div class="row-name">
          Batch Account
        </div>
        <div class="row-info">
          <span class="disable">{{ task.targetBatchAccount }}</span>
        </div>
      </div>
      <div class="row">
        <div class="row-name">
          Target Filters
        </div>
        <div class="row-info">
          <span class="disable">{{ task.targetFilter }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import { DataValidationSchedule } from '@/components/common/schedule-container';

@Component({
  components: {}
})
export default class DataValidationScheduleInfo extends Vue {
  // @Prop() type:ScheduleType
  @Prop()
  task: DataValidationSchedule;
  constructor() {
    super();
  }
}
</script>
<style lang="scss" scoped>
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
</style>
