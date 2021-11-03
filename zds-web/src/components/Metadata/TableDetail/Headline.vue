<template>
  <div class="header">
    <div>
      <h1>{{ name }}</h1>
      <span class="icon-wrapper">
        <span class="sla-icon">SLA/SLE</span>
        <span
          class="idl-icon"
        >IDL</span>
        <ShareLink
          v-model="shareUrl"
          style="position:relative;top:1px;"
        />
      </span>
      <el-rate
        :value="+(rate / 2).toFixed(1)"
        disabled
        show-score
        text-color="#ff9900"
        style="margin-top: 6px;"
      />
    </div>
    <div class="actions">
      <access-control
        v-if="name"
        ref="ac"
        :object-key="groupId"
        auto-create
        :can-edit="canEditAuthGroup"
        style="margin-right: 8px;"
        @group-data="data => authGroup = data"
      >
        <template
          slot="list"
          slot-scope="{ group, loading, onDelete }"
        >
          <!-- hide this temporarily -->
          <div
            v-if="false"
            class="sa-info"
          > 
            <span>
              This table is overseen by 
              <span v-if="editingSA">
                <el-select
                  v-if="canEditAuthGroup"
                  :value="newSA || overseenSA"
                  size="mini"
                  placeholder="Assign to another SA"
                  :remote-method="q => query = q"
                  remote
                  filterable
                  @focus="() => saList.length === 0 && getSAList()"
                  @change="sa => newSA = sa"
                >
                  <el-option
                    v-for="(sa, $i) in SAOptions"
                    :key="$i"
                    :value="sa.sbjct_area"
                  >
                    {{ `${sa.sbjct_area}${sa.sbjct_area_name ? ' - ' + sa.sbjct_area_name : ''}` }}
                  </el-option>
                </el-select>
                <el-button
                  size="small"
                  type="text"
                  @click="setNewSA"
                >
                  Assign
                </el-button>
              </span>
              <a
                v-else
                :href="SALink"
                target="__blank"
              >{{ overseenSA }}</a>
              <el-button
                v-if="canEditAuthGroup && !editingSA"
                size="small"
                type="text"
                icon="el-icon-edit"
                @click="editingSA = true"
              />
            </span>
          </div>
          <auth-list
            slot="list"
            :group="group"
            :loading="loading"
            :on-delete="onDelete"
            :can-edit="canEditAuthGroup"
          />
        </template>
      </access-control>
      <el-button
        type="primary"
        icon="el-icon-edit-outline"
        :disabled="!canEditAuthGroup"
        @click="onEdit"
      >
        Edit
      </el-button>
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Prop, Emit, Watch, Mixins } from 'vue-property-decorator';
import ShareLink from '@/components/common/share-link';
import AccessControl, { Group, GroupIdTemplate } from '@/components/common/AccessControl.vue';
import AuthList from './AuthList.vue';
import Util from '@/services/Util.service';
import { attempt, flattenDeepBy } from '@drewxiu/utils';
import { cloneDeep, keyBy } from 'lodash';
import { TableInfoMixin } from './mixins/TableInfo';
import { namespace, Getter } from 'vuex-class';
import { matchString } from '../utils';

const SaStore = namespace('SAStore');
const SaGroupPrefix = 'metadata:sa-mngmt:';

@Component({
  components: {
    ShareLink,
    AccessControl,
    AuthList,
  },
})
export default class Headline extends Mixins(TableInfoMixin) {

  @Prop() name: string;
  @Prop() rate: number;
  @SaStore.State('saList') saList;
  @SaStore.Action('getSAList') getSAList;
  @Getter('$globalLoading') loading;

  $refs!: {
    ac: AccessControl;
  };

  shareUrl = window.location.href;
  authGroup: Group | null = null;
  query = '';
  newSA = '';
  editingSA = false;

  get SALink () {
    return `${window.location.host}${window.location.pathname}#/metadata/sa/${this.sa}`;
  }
  get SAOptions () {
    return this.saList.filter(i => matchString(this.query, i.sbjct_area));
  }
  get groupId () {
    return GroupIdTemplate.Table(this.name);
  }
  get authorizedNT () {
    return attempt(() => keyBy(flattenDeepBy(cloneDeep(this.authGroup!.items), i => i.children), 'id'), {})!;
  }
  get canEditAuthGroup () {
    const currentUser = Util.getNt();
    return !!this.authGroup && !!this.authorizedNT[currentUser];
  }
  get SAGroup () {
    return this.authGroup!.items.filter(i => i.id.startsWith(SaGroupPrefix));
  }
  get overseenSA () {
    return attempt(() => this.SAGroup[0].id.replace(SaGroupPrefix, ''), '');
  }

  @Watch('authGroup')
  @Watch('sa')
  createDefaultSAGroup () {
    if (!this.authGroup || !this.sa) return;
    if (!this.authGroup.items.find(i => i.id.startsWith('metadata:sa-mngmt:'))) {
      this.createOverseenSA(this.sa)
        .then(() => this.$refs.ac.refresh());
    }
  }

  @Emit('edit')
  onEdit () { /**/ }

  createOverseenSA (sa: string) {
    const id = GroupIdTemplate.SA(sa);
    return AccessControl.api.addGroupItem(this.groupId, [{ id, desc: 'SA Group of Table ' + this.name, type: 'GROUP' }]);
  }
  refresh () {
    this.$refs.ac.refresh();
  }
  async setNewSA () {
    if (!this.newSA || this.newSA === this.overseenSA) {
      return;
    }
    await Promise.all([this.SAGroup.map(g => AccessControl.api.deleteGroupItem(this.groupId, g.id))]);
    await this.createOverseenSA(this.newSA);
    this.editingSA = false;
    this.newSA = '';
    this.refresh();
  }
}
</script>
<style lang="scss" scoped>

.header {
  max-width: calc(100% - 345px);
  display: flex;
  padding: 16px 12px 6px;
  background: white;
  position: sticky;
  top: 0;
  margin-top: 0;
  justify-content: space-between;
  z-index: 10;
  h1 {
    color: #15529A;
    font-weight: normal;
    font-size: 18px;
    display: inline;
  }
  ::v-deep .el-rate__text {
    padding-left: 3px;
  }
  .icon-wrapper {
    ::v-deep * {
      color: #cacbcf !important;
    }
    .sla-icon {
      border: 1px solid #cacbcf;
      border-radius: 4px;
      font-size: 11px;
      padding: 2px 4px;
      margin-left: 5px;
      display: inline-block;
      height: 16px;
      position: relative;
      top: -2px;
    }
    .idl-icon {
      display: inline-block;
      border-radius: 861112px;
      border: 1px solid #cacbcf;
      color: #cacbcf;
      margin-left: 5px;
      margin-right: 3px;
      width: 18px;
      height: 18px;
      line-height: 18px;
      font-size: 8px;
      text-align: center;
      position: relative;
      top: -3px;
    }
  }
  .actions {
    .sa-info {
      margin-bottom: 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      * {
        text-decoration: none;
      }
    }
  }
}
</style>