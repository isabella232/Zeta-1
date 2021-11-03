<template>
  <div class="schedule-container">
    <schedule-wrapper />
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import ScheduleWrapper from '@/components/Schedule/ScheduleWrapper.vue'
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
@Component({
  components: {
    ScheduleWrapper
  }
})

export default class Schedule extends Vue {

  @Provide('notebookRemoteService')
  notebookRemoteService = new NotebookRemoteService()

  constructor() {
    super();
  }
  mounted() {
    this.notebookRemoteService.props({
      path: 'schedule'
    });
  }
}
</script>
