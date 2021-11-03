<template>
  <div class="header">
    <div class="left">
      <a @click="back">
        <i class="el-icon-back" />
      </a>
      <h1>Metadata</h1>
      <i class="el-icon-arrow-right" />
      <h1 style="position:relative;top:1px;">
        {{ name }}
      </h1>
    </div>
    <div class="right">
      <access-control
        v-if="sa"
        :object-key="groupId"
        auto-create
        :can-edit="canEdit"
        @group-data="data => authGroup = data"
      />
      <el-button
        v-if="canEdit"
        type="primary"
        icon="el-icon-edit-outline"
        @click="() => $emit('edit')"
      >
        Edit
      </el-button>
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Prop, Mixins } from 'vue-property-decorator';
import { TableInfoMixin } from './mixins/TableInfo';
import AccessControl, { GroupIdTemplate } from '@/components/common/AccessControl.vue';
import Util from '@/services/Util.service';


@Component({
  components: {
    AccessControl,
  },
})
export default class NavBar extends Mixins(TableInfoMixin) {

  @Prop() name: string;

  authGroup = null;

  get groupId () {
    return GroupIdTemplate.SA(this.sa);
  }

  get canEdit () {
    if (!this.authGroup) {
      return false;
    }
    const nt = Util.getNt();
    return !!(this.authGroup as any).items.find(i => i.id === nt);
  }

  back () {
    this.$router.back();
  }

}
</script>
<style lang="scss" scoped>

$gray: #CACBCF;

.header {
  display: flex;
  width: 100%;
  justify-content: space-between;
  padding: 14px 12px;
  margin-top: 0;
  border-bottom: 2px solid $gray;
  box-sizing: border-box;
  position: sticky;
  top: 0;
  z-index: 99;
  background: white;

  .left {
    h1 {
      display: inline-block;
      font-size: 18px;
      &:first-of-type::before {
        content: '';
        display: inline-block;
        height: 20px;
        width: 1px;
        background: $gray;
        margin: 0 14px;
        vertical-align: text-bottom;
      }
    }
  }
  .right {
    display: flex;
    justify-content: center;
    color: $gray;
    > * {
      margin-left: 12px;
    }
  }
}
</style>