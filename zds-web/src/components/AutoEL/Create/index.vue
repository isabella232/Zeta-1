<template>
  <div class="auto-el-list">
    <el-header class="header">
      <div class="title">
        <h1 @click="goTaskList">
          Task List
        </h1>
        <h1>{{ title }}</h1>
      </div>
    </el-header>
    <el-steps
      :active="step"
      align-center
    >
      <el-step title="Select Table" />
      <el-step title="Data Model" />
      <el-step title="Preview" />
      <el-step title="Release" />
      <el-step title="Post-Release" />
    </el-steps>
    <div class="content-wrapper">
      <router-view />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import AutoELModule, { Actions, Mutations } from '../store';
import { Action, Mutation, State } from 'vuex-class';

@Component({
  components: {},
})
export default class TaskDetail extends Vue {
  @Action(Actions.GetTask) getTask;
  @Mutation(Mutations.Clear) clear;
  @State(state => state.AutoEL.task) task;

  @Prop() preview;

  get title () {
    const { id } = this.$route.params;
    if (!id) {
      return 'Create Job';
    }
    switch (this.step) {
      case 0:
        return 'Edit Table Info';
      case 1:
        return 'Edit Data Model';
      case 2:
        return 'Preview Data Model';
      case 3:
        return 'Edit Release Info';
      case 4:
        return 'Post-release Status';
    }
  }
  get step () {
    switch (this.$route.name) {
      case 'Create New Task':
      case 'Select Table':
        return 0;
      case 'Data Model':
        return this.$route.query.preview ? 2 : 1;
      case 'Release Task':
        return 3;
      case 'Post Release':
      default:
        return 4;
    }
  }
  goTaskList () {
    this.$router.push('/autoel');
  }
  beforeCreate () {
    if (!this.$store.state[AutoELModule.namespace]) {
      this.$store.registerModule(AutoELModule.namespace, AutoELModule);
    }
  }
  mounted () {
    const { id } = this.$route.params;
    if (id) {
      this.getTask(id);
    }
  }
  beforeDestroy () {
    this.clear();
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';

.auto-el-list {
  padding: 16px 8px;
  .header {
    padding: 8px 0;
    display: flex;
    justify-content: space-between;
    .title {
      h1 {
        font-size: 16px;
        display: inline-block;
        color: $header-color;
        &:not(:last-of-type) {
          font-weight: normal;
          cursor: pointer;
          &::after {
            content: '/';
            display: inline-block;
            padding: 0 6px;
          }
        }
      }
    }
  }
}
</style>
