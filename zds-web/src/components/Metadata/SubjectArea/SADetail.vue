<template>
  <div class="sa-detail-wrapper">
    <Header
      :current-page="form.sbjct_area || 'New Subject Area'"
      :show-back-button="true"
      :breadcrumb="[{ key: 'Metadata', label: 'Metadata' }, { key: 'SA', label: 'Subject Areas'}, { key: 'SADetail', label: form.sbjct_area || 'New Subject Area'}]"
      @breadcrumb-click="backTo"
    />
    <h1>Subject Area: {{ form.sbjct_area }}</h1>
    <h2> <i class="el-icon-s-data" /> Overview </h2> 
    <div class="btn-group"> 
      <access-control
        v-if="saDetail"
        :object-key="groupId"
        auto-create
        :can-edit="canEditAuthGroup"
        :after-add="afterAddAuthMember"
        :before-delete="beforeDelAuthMember"
        @group-data="data => authGroup = data"
      />
      <div v-if="isEditing"> 
        <el-button
          plain
          @click="cancel"
        >
          Cancel
        </el-button>

        <el-button
          type="success"
          icon="el-icon-check"
          :loading="loading"
          @click="() => isNew ? create() : save()"
        >
          Save
        </el-button>
      </div>
      <div v-else> 
        <el-button
          type="primary"
          plain
          icon="el-icon-edit-outline"
          :disabled="!canEditAuthGroup"
          @click="() => isEditing = true"
        >
          Edit
        </el-button> 
      </div>
      <div>
        <el-tooltip
          effect="light"
          content="Only members of this Access Group can edit the subject area. Also, A user must be added to the Access Control group before being assigned to any role of this subject area."
        >
          <i
            class="el-icon-info"
            style="color:#aaa;pointer:cursor;"
          />
        </el-tooltip>
      </div>
    </div>

    <el-divider />
    <div class="detail-content">
      <el-form
        ref="form"
        :model="form"
        label-width="180px"
        label-position="left"
      >
        <el-row
          v-if="isNew"
          :gutter="100"
        >
          <el-col :span="12">
            <el-form-item
              prop="sbjct_area"
              label="Subject Area"
              :rules="[{ required: true, message: 'Please input a Subject Area', trigger: ['blur', 'change'] }]"
            >
              <el-input
                v-model="form.sbjct_area"
                placeholder="Enter the name of the subject area"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="100">
          <el-col :span="12">
            <el-form-item
              prop="dev_mngr_nt"
              label="Dev Manager"
              :rules="[{ required: true, message: 'Please choose a Dev Manager', trigger: ['blur', 'change'] }]"
            >
              <user-selection
                :nt="form.dev_mngr_nt"
                :name="form.dev_mngr"
                :disabled="!isEditing"
                :custom-options="userOptions"
                @change="val => { form.dev_mngr_nt = val.nt; form.dev_mngr = val.fullName }"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              prop="prdct_owner_nt"
              label="Product Owner"
              :rules="[{ required: true, message: 'Please choose a Product Owner', trigger: ['blur', 'change'] }]"
            >
              <user-selection
                :nt="form.prdct_owner_nt"
                :name="form.prdct_owner"
                :disabled="!isEditing"
                :custom-options="userOptions"
                @change="val => { form.prdct_owner_nt = val.nt; form.prdct_owner = val.fullName }"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider />

        <el-row :gutter="100">
          <el-col :span="12">
            <el-form-item label="BSA/PM">
              <multi-user-selection 
                name="bsa"
                :nt="form.bsa.map(i => i.nt)"
                :name-map="getNameMap(form.bsa)"
                :disabled="!isEditing"
                :custom-options="userOptions"
                @change="val => form.bsa = val.map(i => ({ nt: i.nt, name: i.fullName }))"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Dev SAE">
              <multi-user-selection 
                name="dev"
                :nt="form.dev.map(i => i.nt)"
                :name-map="getNameMap(form.dev)"
                :disabled="!isEditing"
                :custom-options="userOptions"
                @change="val => form.dev = val.map(i => ({ nt: i.nt, name: i.fullName }))"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="100">
          <el-col :span="12">
            <el-form-item label="Data Architect">
              <user-selection
                :nt="form.prmry_da_nt"
                :name="form.prmry_da"
                :disabled="!isEditing"
                :custom-options="daOptions"
                @change="val => { form.prmry_da_nt = val.nt; form.prmry_da = val.fullName }"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Data Steward">
              <multi-user-selection 
                name="data_owner"
                :nt="form.data_owner.map(i => i.nt)"
                :name-map="getNameMap(form.data_owner)"
                :disabled="!isEditing"
                :custom-options="userOptions"
                @change="val => form.data_owner = val.map(i => ({ nt: i.nt, name: i.fullName }))"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider />

        <el-row>
          <el-col :span="24">
            <el-form-item
              prop="sa_desc"
              label="Description"
              :rules="[{ required: true, message: 'Please input the description of the Subject Area', trigger: ['blur', 'change'] }]"
            >
              <el-input
                v-if="isEditing"
                v-model="form.sa_desc"
                type="textarea"
                style="width:100%"
              /> 
              <p v-else>
                {{ form.sa_desc }}
              </p>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider />

        <el-row :gutter="100">
          <el-col :span="12">
            <el-form-item label="Domain / Subdomain">
              <el-cascader
                v-if="isEditing"
                :options="domainOptions"
                :value="[form.domain, form.sub_domain].filter(Boolean)"
                style="width:100%"
                @change="onSelectDomain"
              />
              <span v-else> {{ form.domain }}{{ form.sub_domain ? ` / ${form.sub_domain}` : '' }}</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="100">
          <el-col :span="12">
            <el-form-item label="Batch Account">
              <el-input
                v-if="isEditing"
                v-model="form.batch_acct"
              /> 
              <span v-else> {{ form.batch_acct }} </span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SA Code">
              <el-input
                v-if="isEditing"
                v-model="form.sa_code"
              /> 
              <span v-else> {{ form.sa_code }} </span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="100">
          <el-col :span="12">
            <el-form-item label="Production Database">
              <el-autocomplete 
                v-if="isEditing"
                v-model="form.target_db"
                :fetch-suggestions="(q, cb) => fetchDB(q, cb, true)"
                style="width:100%"
              />
              <span v-else> {{ form.target_db }} </span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Working Database">
              <el-autocomplete 
                v-if="isEditing"
                v-model="form.target_working_db"
                :fetch-suggestions="(q, cb) => fetchDB(q, cb, false)"
                style="width:100%"
              />
              <span v-else> {{ form.target_working_db }} </span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <el-button 
      v-if="!isNew" 
      type="text"
      plain
      icon="el-icon-document" 
      style="margin-top:30px;"
      @click="goTableList"
    > 
      View Table List 
    </el-button>

    <div
      v-if="showCard"
      class="user-popover"
      :style="{left: hoveredUser.x + 'px', top: hoveredUser.y + 'px'}"
      @mouseenter="() => showCard += 1"
      @mouseleave="mouseLeave"
    >
      <user-card :nt="hoveredUser.nt" />
    </div>
  </div>
</template>

<script lang="ts">
import { Vue, Component, Watch } from 'vue-property-decorator';
import UserSelection from '@/components/common/UserSelection.vue';
import MultiUserSelection from '@/components/common/MultiUserSelection.vue';
import { Getter, namespace } from 'vuex-class';
import Util from '@/services/Util.service';
import UserCard from '@/components/common/UserCard.vue';
import AccessControl, { Group, GroupIdTemplate } from '@/components/common/AccessControl.vue';
import { SaDetail as ISaDetail } from '@/types/metadata/sa';
import _, { keyBy, uniqBy } from 'lodash';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import * as TYPE from '@/stores/MutationTypes';
import { attempt, isEmpty } from '@drewxiu/utils';
import { getSAChanges, sendEmailForAddNewAuthMembers, sendEmailForRoleChange, sendEmailToDeletedUser } from './helper';
import Header from '@/components/Metadata/index/components/Header.vue';

const SaStore = namespace('SAStore');

@Component({
  components: {
    MultiUserSelection,
    UserCard,
    UserSelection,
    AccessControl,
    Header,
  },
})
export default class SADetail extends Vue {
  @SaStore.Action('getSADetail') getSADetail;
  @SaStore.Action('saveSADetail') saveSADetail;
  @SaStore.State('saDetail') saDetail: ISaDetail;
  @SaStore.Action('getDAList') getDAList;
  @SaStore.State('daList') daList;
  @SaStore.Mutation('clear') clear;
  @Getter('$globalLoading') loading;

  isEditing = false;
  showCard = 0;
  hoveredUser = {
    nt: '',
    x: 0,
    y: 0,
  };
  form: ISaDetail = {
    sbjct_area: '',
    sbjct_area_name: '',
    lctn_desc: '',
    sa_desc: '',
    dev_mngr: '',
    dev_mngr_nt: '',
    prdct_owner: '',
    prdct_owner_nt: '',
    pm_mngr: '',
    pm_mngr_nt: '',
    prmry_da: '',
    prmry_da_nt: '',
    data_owner: [],
    start_dt: null,
    end_dt: '',
    cre_date: '',
    cre_user: '',
    upd_date: '',
    upd_user: '',
    domain: '',
    sub_domain: '',
    sa_code: '',
    batch_acct: '',
    target_db: '',
    target_working_db: '',
    dev: [],
    bsa: [],
  };
  authGroup: Group | null = null;
  domainInfo: any = [];
  get groupId () {
    const sa = this.isNew ? this.form.sbjct_area : this.saDetail.sbjct_area;
    return GroupIdTemplate.SA(sa);
  }
  get authorizedNT () {
    return attempt(() => keyBy(this.authGroup!.items, 'id'), {})!;
  }
  get userOptions () {
    return attempt(() => this.authGroup!.items.map(i => ({ nt: i.id, fullName: i.desc, label: i.desc})), []);
  }
  get daOptions () {
    return this.daList.filter(da => da.role === 'da').map(da => ({ nt: da.nt, fullName: da.name, label: `${da.name}(${da.nt})`}));
  }
  get canEditAuthGroup () {
    const currentUser = Util.getNt();
    return !!this.authGroup && !!this.authorizedNT[currentUser];
  }
  get formRef () {
    return this.$refs['form'] as any;
  }
  get isNew () {
    return this.$route.params.id === 'new';
  }
  async created () {
    this.getDAList();
    if (this.isNew) {
      this.isEditing = true;
    } else {
      this.getSADetail(this.$route.params.id);
    }
  }
  get domainOptions () {
    return this.domainInfo.map(d => {
      return {
        label: d.domain,
        value: d.domain,
        children: d.sub_domain ? d.sub_domain.split(',').map(s => ({ label: s, value: s })) : null,
      };
    });
  }
  beforeDestroy () {
    this.clear();
  }
  @Watch('isEditing')
  loadOptions (isEditing) {
    if (isEditing) {
      isEmpty(this.getDomainInfo) && this.getDomainInfo();
    }
  }
  @Watch('saDetail')
  reset () {
    if (!_.isEmpty(this.saDetail)) {
      this.form = Object.assign({}, this.saDetail);
    }
    this.formRef.clearValidate();
  }
  cancel () {
    if (this.isNew) {
      return this.backTo('SA');
    }
    this.reset();
    this.isEditing = false;
  }
  get allUsers () {
    const {
      dev_mngr,
      dev_mngr_nt,
      prdct_owner_nt,
      prdct_owner,
      prmry_da_nt,
      prmry_da,
      data_owner,
      dev,
      bsa,
    } = this.form;
    return uniqBy([
      [dev_mngr_nt, dev_mngr],
      [prdct_owner_nt, prdct_owner],
      [prmry_da_nt, prmry_da],
      ...data_owner.map(i => [i.nt, i.name]),
      ...dev.map(i => [i.nt, i.name]),
      ...bsa.map(i => [i.nt, i.name]),
    ].filter(i => i[0]), i => i[0]);
  }
  async create () {
    this.formRef.validate(async isValid => {
      if (!isValid) {
        return; // todo, validation rules?
      }
      const initAuthGroup = this.allUsers.map(([nt, name]) => ({
        id: nt,
        groupId: this.groupId,
        desc: `${name}(${nt})`,
      }));
      await this.saveSADetail(this.form);
      await AccessControl.api.createGroup(this.groupId, this.groupId);
      await AccessControl.api.addGroupItem(this.groupId, initAuthGroup);
      this.isEditing = false;
      this.$message.success('Subject Area successfully created.');
      this.$router.push(`/metadata/sa/${this.form.sbjct_area}`);
    });
  }
  async save () {
    this.formRef.validate(async isValid => {
      if (!isValid) {
        return; // todo, validation rules?
      }
      const unauthorized = this.allUsers.filter(([nt]) => !!nt && !this.authorizedNT[nt]);
      if (unauthorized.length > 0) {
        try {
          await this.$confirm(
            `<div style="white-space:pre-wrap">These users:\n<div style="font-weight:bold;">${unauthorized.map(([nt, name]) => `\n${name}(${nt})`)}</div>\nare not in the Access Control group, would you like to add them to the group and proceed?</div>`,
            'Unautorized NT',
            { dangerouslyUseHTMLString: true }
          );
          const groupItems = unauthorized.map(([nt, name]) => ({
            id: nt,
            groupId: this.groupId,
            desc: `${name}(${nt})`,
          }));
          await AccessControl.api.addGroupItem(this.groupId, groupItems);
        } catch (e) {
          return;
        }
      }
      await this.saveSADetail(this.form);
      this.isEditing = false;
      this.$message.success('Subject Area successfully saved.');
      const changes = getSAChanges(this.saDetail, this.form);
      if (changes.length > 0) {
        sendEmailForRoleChange(this.userOptions!.map(i => i.nt), changes, this.saDetail.sbjct_area);
      }
    });
  }
  backTo (route) {
    if (route === 'Metadata') this.$router.push('/metadata');
    if (route === 'SA') this.$router.push('/metadata/sa');
  }

  goTableList () {
    this.$store.commit(TYPE.SET_TABLE_MANAGEMENT_ROUTE, {
      data: {
        sbjct_area: this.saDetail.sbjct_area,
      },
    });
    this.$router.push('/tablemanagement?sbjct_area=' + JSON.stringify([this.saDetail.sbjct_area]));
  }
  mouseLeave () {
    setTimeout(() => {
      this.showCard -= 1;
    }, 300);
  }
  showUserCard (evt, nt) {
    this.showCard += 1;
    const bound = evt.target.getBoundingClientRect();
    Object.assign(this.hoveredUser, {
      nt,
      x: bound.x - 25,
      y: bound.y + 30,
    });
  }
  onSelectDomain ([domain, subDomain]) {
    this.form.domain = domain;
    this.form.sub_domain = subDomain || null;
  }
  getNameMap (userInfo) {
    return userInfo.reduce((prev, next) => {
      prev[next.nt] = next.name;
      return prev;
    }, {});
  }
  async getDomainInfo () {
    this.domainInfo = ((await DoeRemoteService.instance.getDomainInfo()) as any).data.data.value;
  }

  fetchDB (queryStr, cb, isProd) {
    if (!queryStr) {
      return cb([]);
    }
    DoeRemoteService.instance.getDBList(queryStr, isProd ? 1 : undefined, isProd ? undefined : 1).then(data => {
      cb(data.map(i => ({ value: i.db_name })));
    });
  }
  afterAddAuthMember (nt: string[]) {
    sendEmailForAddNewAuthMembers(nt, this.saDetail.sbjct_area);
  }
  beforeDelAuthMember (nt) {
    if (this.allUsers.find(i => i[0] === nt)) {
      this.$alert('This user has an active role in this Subject Area, please remove this user\'s role first', 'Cannot delete this user');
      return false;
    }
    sendEmailToDeletedUser(nt, this.saDetail.sbjct_area);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
@import '@/styles/metadata.scss';

.sa-detail-wrapper {
  height: 100vh;
  padding: 8px 16px 0;
  box-sizing: border-box;

  .el-select,
  .el-input {
    vertical-align: middle;
  }
  .el-breadcrumb__item {
    &:not(:last-of-type) {
      cursor: pointer;
      span {
        color: #4d8cca;
        &:hover {
          text-decoration: underline;
        }
      }
    }
  }

  h1 {
    margin: 24px 0 12px;
    font-size: 18px;
    font-style: normal;
    font-weight: 700;
  }
  h2 {
    margin-top: 12px;
    font-size: 13px;
    font-style: normal;
    font-weight: 700;
    color: #4d8cca;
    display: inline-block;
  }
  .btn-group {
    float: right;
    div {
      display: inline-block;
      margin-left: 12px;
    }
  }
  /deep/ .el-form-item__label {
    font-weight: 700;
  }

  .user-popover {
    position: fixed;
    z-index: 2000;
    background: white;
    border: 1px solid #999;
    padding: 1em 1.5em;
    border-radius: 8px;
    min-width: 200px;
    &::before {
      content: '';
      display: block;
      height: 12px;
      width: 12px;
      transform: rotate(-45deg);
      border: 1px solid #999;
      border-left: transparent;
      border-bottom: transparent;
      position: absolute;
      top: -7px;
      left: 80px;
      background: white;
    }
  }
}
</style>
<style lang="scss">
.domain-suggestions {
  width: 350px !important;
}
</style>
