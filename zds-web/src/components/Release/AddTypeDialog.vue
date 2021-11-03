<template>
  <el-dialog label-width="80px" title="Add File" :visible.sync="visible_">
    <el-input prefix-icon="el-icon-search" v-model="filter"></el-input>
    <el-checkbox-group v-model="checkedOptions">
      <div v-for="item in filterOptions" :key="item.id">
        <el-checkbox :disabled="disabled(item.id)" :label="item" :value="item">{{item.path + item.title}}</el-checkbox>
      </div>
    </el-checkbox-group>
    <div slot="footer" class="dialog-footer">
      <el-button id="add-btn" type="primary" @click="add()">Add</el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import _ from 'lodash';
@Component({
  components: {}
})
export default class AddTypeDialog extends Vue {
  @Prop() visible: boolean;
  @Prop() data: Array<any>;
  @Prop() type: string;
  @Prop() checked: Array<any>;
  @Prop() disabledOptions: Array<any>;

  filter: string = "";
  checkedOptions: Array<any> = [];

  set visible_(e) {
    this.$emit("update:visible", e);
  }

  get visible_() {
    if (!this.visible) this.filter = "";
    return this.visible;
  }

  get filterOptions() {
    return _.filter(this.data, o => {return (o.path + o.title).toLowerCase().indexOf(this.filter.toLowerCase()) > -1;});
  }

  disabled(id: string) {
    const find = _.find(this.disabledOptions, o => o.id == id)
    return !_.isUndefined(find);
  }

  add() {
    this.$emit("add-file", this.checkedOptions, this.type);
    this.$emit("update:visible", false);
  }

  @Watch('checked')
  checkedChange(newVal: Array<any>) {
    this.checkedOptions = [];
    _.forEach(newVal, (o: any) => {
      const find = _.find(this.checkedOptions, (v: any) => v.id == o.id);
      if (_.isUndefined(find)) {
        const find = _.find(this.data, (v: any) => v.id == o.id);
        if (!_.isUndefined(find)) this.checkedOptions.push(find);
      }
    })
  }
}
</script>
<style lang="scss" scoped>
.el-dialog {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.el-input {
  margin-bottom: 22px;
}
.el-checkbox-group {
  height: 300px;
  overflow-y: auto;
}
</style>
