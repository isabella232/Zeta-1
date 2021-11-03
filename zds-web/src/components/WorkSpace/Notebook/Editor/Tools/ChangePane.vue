<template>
  <div class="panel-command">
    <el-dropdown
      trigger="click"
      class="panel-command-dropdown"
      @command="handleViewCommand"
    >
      <span
        class="el-dropdown-link"
        title="Move Panel"
      >
        <i class="zeta-icon zeta-icon-horizontal" />
      </span>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item command="toggle-result">
          <i class="zeta-icon-codeonly" />{{ layout.display==='none'?'Show Result':'Code Only' }}
        </el-dropdown-item>
        <el-dropdown-item command="change-horizontal">
          <i class="zeta-icon-horizontal" />Horizontal View
        </el-dropdown-item>
        <el-dropdown-item command="change-vertical">
          <i class="zeta-icon-vertical" />Vertical View
        </el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import _ from 'lodash';
import { IPreference, INotebook } from '@/types/workspace';

@Component
export default class ChangePane extends Vue {
  @Prop() notebookId: string;
  @Inject() debouncedSavePreference: Function;

  get preference (): IPreference | undefined {
    const notebook: INotebook = this.$store.state.workspace.workspaces[
      this.notebookId
    ];
    if (notebook) {
      return notebook.preference;
    } else {
      return undefined;
    }
  }
  get layout () {
    const currentLayout =  this.preference && this.preference['notebook.layout'] ?
      this.preference['notebook.layout']
      :{
        display: 'horizontal',
        lastDisplay: 'horizontal',
        horizontal: 60,
        vertical: 60,
      };
    return currentLayout;
  }
  mounted () {
    this.$store.dispatch('updateNotebookLayout', {
      notebookId: this.notebookId,
      layout: this.layout,
    });
  }
  handleViewCommand (command: string){
    switch (command){
      case 'toggle-result':
        this.toggleResult();
        break;
      case 'change-horizontal':
        this.changeView('horizontal');
        break;
      case 'change-vertical':
        this.changeView('vertical');
        break;
    }
  }
  changeView (display: string){
    const layout = Object.assign(this.layout,  {display: display, lastDisplay: display});
    this.updatePreference(layout);
  }
  toggleResult (){
    if (this.layout.display === 'none'){
      const layout = Object.assign(this.layout,  {display: this.layout.lastDisplay});
      this.updatePreference(layout);
    } else {
      // update preference
      const  layout = Object.assign(this.layout, {display: 'none'});
      this.updatePreference(layout);
    }
  }
  updatePreference (layout: any) {
    const preference: IPreference = this.preference? this.preference :{};
    preference['notebook.layout'] = layout;

    this.$store.dispatch('updateNotebookLayout', {
      notebookId: this.notebookId,
      layout: layout,
    });
    this.debouncedSavePreference();
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/global.scss";
.panel-command{
  margin-right: 10px;
  .el-dropdown-link{
    line-height: 1;
  }
  i.zeta-icon{
    cursor: pointer;
    font-size: 16px;
    color: $zeta-global-color;
  }
}
</style>
