<template>
  <div
    v-loading="loading"
    class="schedule-share-page"
  >
    <ReadOnly :message="message" />
    <div
      v-if="info"
      class="header"
    >
      <div class="tool">
        <el-tooltip
          placement="bottom"
          effect="light"
        >
          <div slot="content">
            Request for schedule edit access and we will send an email to owner for approval.
          </div>
          <el-button
            v-click-metric:ZS_CLICK="{name: 'Request for access'}"
            v-loading="applyLoading"
            type="primary"
            @click="apply"
          >
            Request Edit
          </el-button>
        </el-tooltip>
      </div>
    </div>
    <ScheduleDetail
      v-if="info"
      :read-only="true"
      :info="info"
    />
    <div
      v-else
      class="no-data"
    >
      Not found this schedule.
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import ScheduleDetail from '@/components/Schedule/schedule-detail.vue';
import ReadOnly from '@/components/WorkSpace/Notebook/Editor/Tools/ReadOnly.vue';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import Util from '@/services/Util.service';

@Component({
  components:{
    ScheduleDetail,
    ReadOnly,
  },
})
export default class ScheduleDetailView extends Vue {
  notebookRemoteService = new NotebookRemoteService();
  message = 'This Schedule is Read Only';
  loading = false;
  applyLoading = false;
  info = null;
  jobId = 0;
  created () {
    this.jobId = Number(this.$route.params.id);
    this.init();
  }
  async init(){
    this.loading = true;
    const data = await this.$store.dispatch('getJobById', this.jobId);
    this.loading = false;
    this.info = data;
  }
  async apply() {
    try {
      const nt = Util.getNt();
      this.applyLoading = true;
      const response = await this.notebookRemoteService.applyForAccess(nt, this.jobId);
      this.applyLoading = false;
      if(response.data&& response.data==='Success'){
        this.$message({
          message: 'Request Accepted!',
          type:'success',
        });
      }else{
        this.$message({
          message: response.data.msg,
          type:'error',
        });
      }
    }
    catch(e) {
      this.applyLoading = false;
      this.$message({
        message: 'Fail, please refresh page and try again',
        type:'error',
      });
    }
  }
}
</script>
<style lang="scss" scope>
  .schedule-share-page{
    .schedule-detail-page{
      height: calc(100% - 70px);
      overflow-y: auto;
      margin-right: -10px;
      .schedule-container{
        margin-top: 0;
      }
    }
    .header {
      margin: 10px 0;
      .tool {
        flex: 1;
        text-align: right;
        cursor: pointer;

      }
    }
    .no-data{
      text-align: center;
      font-size: 14px;
    }
  }

</style>
