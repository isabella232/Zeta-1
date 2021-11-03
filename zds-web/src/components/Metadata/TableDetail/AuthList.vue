<template>
  <div
    v-loading="loading"
    class="table-auth-list"
  >
    <template v-if="SAGroup">
      <div
        v-for="i in SAGroup.children"
        :key="i.id"
        class="group-item"
      >
        <i class="zeta-icon-user" />
        <span class="name">{{ i.id }}</span>
        <span class="desc">{{ i.desc }}</span>
        <span class="sa-tag">SA Owner</span>
        <el-divider />
      </div>
    </template>
    <div
      v-for="(i, $index) in users"
      :key="$index"
      class="group-item"
    >
      <i class="zeta-icon-user" />
      <span class="name">{{ i.id }}</span>
      <span class="desc">{{ i.desc }}</span>
      <el-popconfirm
        v-if="canEdit"
        title="Remove this person from the group?"
        confirm-button-text="Ok"
        cancel-button-text="Cancel"
        @onConfirm="() => onDelete(i.id)"
      >
        <el-button
          slot="reference"
          icon="el-icon-close"
          size="mini"
          class="delete-icon"
          circle
        />
      </el-popconfirm>
      <el-divider />
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Mixins, Prop } from 'vue-property-decorator';
import { TableInfoMixin } from './mixins/TableInfo';

@Component({
  components: {
  },
})
export default class AuthList extends Mixins(TableInfoMixin) {

  @Prop() group: any;
  @Prop() loading: boolean;
  @Prop() canEdit: boolean;
  @Prop() onDelete: Function;

  get SAGroup () {
    return this.group.items.find(i => i.id.startsWith('metadata:sa-mngmt:'));
  }

  get users () {
    return this.group.items.filter(i => !i.type);
  }
}
</script>
<style lang="scss" scoped>
.table-auth-list {
  padding-bottom: 24px;
  .group-item {
    i {
      font-size: 18px;
    }
    font-size: 16px;
    .name {
      padding: 0 1em;
    }
    .desc {
      color: #bbb;
    }
    .delete-icon {
      border: none;
      float: right;
      background-color: transparent;
    }
    .sa-tag {
      padding: 2px 4px;
      border-radius: 5px;
      color: white;
      font-size: 10px;
      background: #5cb85c;
      float: right;
      position: relative;
      top: 5px;
    }
  }
}
</style>
<style lang="scss">
.table-auth-list {
  .el-divider {
    margin: 16px 0;
  }
}
</style>