<template>
  <div
    v-show="show"
    class="access-control"
  >
    <el-button
      type="primary"
      plain
      :disabled="disabled"
      @click="onOpen"
    >
      {{ buttonText }}
    </el-button>
    <el-dialog
      width="650px"
      :visible="showDialog"
      :modal-append-to-body="false"
      @close="onClose"
    >
      <div
        slot="title"
        class="title"
      >
        <i class="el-icon-s-custom" />
        Manage Accessbility
      </div>
      <div
        v-if="canEdit"
        class="user-input"
      >
        <multi-user-selection
          :nt="newItems"
          :name-map="itemMap"
          @change="
            (val) => {
              newItems = val.map((i) => i.nt);
              itemMap = val.reduce((prev, next) => {
                prev[next.nt] = next.fullName;
                return prev;
              }, {});
            }
          "
        />
        <el-button
          size="small"
          type="primary"
          style="margin-left: 8px"
          @click="onAdd"
        >
          Add
        </el-button>
      </div>
      <el-divider />
      <slot
        name="list"
        :group="group"
        :loading="loading"
        :onDelete="onDelete"
      >
        <div
          v-if="group"
          v-loading="loading"
          class="item-wrapper"
        >
          <div
            v-for="i in group.items"
            :key="i.id"
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
      </slot>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Emit } from 'vue-property-decorator';
import MultiUserSelection from '@/components/common/MultiUserSelection.vue';
import GroupService, { Group } from '@/services/remote/GroupService';
import { noop } from 'vue-class-component/lib/util';
const GroupIdTemplate = {
  SA: (sa) => `metadata:sa-mngmt:${sa}`,
  Table: (table) => `metadata:table-mngmt:${table}`,
};
export { Group, GroupIdTemplate };

const API = GroupService.instance;

@Component({
  components: {
    MultiUserSelection,
  },
})
export default class AccessControl extends Vue {
  static api = API;
  @Prop() objectKey: string;
  @Prop({ default: 'Access Control' }) buttonText: string;
  @Prop({
    type: Boolean,
    default: false,
  })
  autoCreate: boolean;
  @Prop({
    type: Boolean,
    default: false,
  })
  disabled: boolean;
  @Prop({
    type: Boolean,
    default: true,
  })
  show: boolean;
  @Prop({
    type: Boolean,
    default: true,
  })
  canEdit: boolean;
  @Prop({
    type: Boolean,
    default: true,
  })
  lazy: boolean;
  @Prop({ default: '' }) description: string;
  @Prop({ default: () => noop }) beforeAdd: Function;
  @Prop({ default: () => noop }) afterAdd: Function;
  @Prop({ default: () => noop }) beforeDelete: Function;
  @Prop({ default: () => noop }) afterDelete: Function;

  showDialog = false;
  loading = false;
  group: Group | null = null;
  newItems: string[] = [];

  itemMap = {};

  @Watch('group')
  @Emit('group-data')
  emitGroupData () {
    return this.group;
  }
  @Watch('objectKey', { immediate: true })
  loadGroup (key) {
    if (!this.lazy && key) {
      this.refresh();
    }
  }
  async onOpen () {
    this.showDialog = true;
    this.refresh();
  }
  async onAdd () {
    const { beforeAdd, afterAdd } = this;
    const items = [...this.newItems];
    const proceed = beforeAdd && (await beforeAdd(items, this.objectKey));
    if (proceed === false) {
      console.warn('Action (Add Member) aborted explicitly.');
      return;
    }
    await API.addGroupItem(
      this.objectKey,
      items.map((id) => {
        return {
          id,
          desc: `${this.itemMap[id]}(${id})`,
        };
      })
    );
    afterAdd && afterAdd(items, this.objectKey);
    this.newItems = [];
    this.refresh();
  }

  async refresh () {
    this.loading = true;
    this.group = await API.getGroup(this.objectKey);
    this.loading = false;
  }

  async onDelete (id) {
    const { beforeDelete, afterDelete } = this;
    const proceed = beforeDelete && (await beforeDelete(id, this.objectKey));
    if (proceed === false) {
      console.warn('Action (Delete Member) aborted explicitly.');
      return;
    }
    await API.deleteGroupItem(this.objectKey, id);
    this.refresh();
    afterDelete(id, this.objectKey);
  }

  onClose () {
    this.showDialog = false;
    this.newItems = [];
  }

  async created () {
    const { objectKey, autoCreate, description } = this;

    this.group = await API.getGroup(objectKey).catch((e) => {
      e.resolved = true;
      return null;
    });
    if (!this.group) {
      if (autoCreate) {
        this.group = await API.createGroup(objectKey, description || objectKey);
      } else {
        throw new Error(`Object Key ${objectKey} does not exists`);
      }
    }
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.access-control {
  display: inline-block;
  .title {
    color: #333;
    font-size: 18px;
    font-weight: bold;
    > i {
      color: white;
      background: #569ce1;
      display: inline-block;
      border-radius: 861112px;
      padding: 5px;
      margin-right: 8px;
    }
  }
  .user-input {
    display: flex;
    align-items: center;
    margin-top: 16px;
  }
  .item-wrapper {
    padding-bottom: 18px;
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
    }
  }
}
</style>


