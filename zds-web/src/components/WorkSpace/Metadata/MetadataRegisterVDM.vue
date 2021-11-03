<template>
  <div
    v-loading="loading"
    class="register"
  >
    <div class="steps-bar">
      <el-steps
        :active="active"
        finish-status="success"
        simple
      >
        <el-step title="STEP 1" />
        <el-step title="STEP 2" />
        <el-step title="STEP 3" />
      </el-steps>
    </div>
    <div class="main-content">
      <register-step-one
        v-if="active == 0"
        id="step-one"
        ref="one"
        @ready-do-next="() => active += 1"
      />
      <register-step-two
        v-if="active == 1"
        id="step-two"
        ref="two"
        @ready-do-next="() => active += 1"
      />
      <register-step-three
        v-if="active == 2"
        id="step-three"
        ref="three"
        @ready-do-next="() => active == 2"
      />
    </div>
    <div class="button-group">
      <div
        v-if="active < 1"
        class="blank-div"
      />
      <el-button
        v-if="active >= 1"
        class="pre-btn"
        type="default"
        plain
        @click="lastStep"
      >
        Previous
      </el-button>
      <el-button
        v-if="active < 2"
        class="next-btn"
        type="primary"
        @click="nextStep"
      >
        Next
      </el-button>
      <el-button
        v-if="active == 2"
        class="submit-btn"
        type="primary"
        @click="preSubmit"
      >
        Submit
      </el-button>
    </div>
    <el-dialog
      :visible.sync="dialogVisible"
      :close-on-click-modal="false"
      width="333px"
      @close="add"
    >
      <span
        slot="title"
        class="dialog-header"
      >
        <i class="zeta-icon-finish" />
        <span>Success!</span>
      </span>
      <span>Your VDM has been submitted.</span>
      <span
        slot="footer"
        class="dialog-footer"
      >
        <el-button
          type="default"
          plain
          @click="add"
        >Add Another</el-button>
        <el-button
          type="default"
          plain
          @click="view"
        >View</el-button>
      </span>
    </el-dialog>
    <el-dialog
      :visible.sync="sampleQueryDialogVisible"
      :close-on-click-modal="false"
      :show-close="false"
      width="333px"
    >
      <span>You may have an unsaved sample query.</span>
      <span
        slot="footer"
        class="dialog-footer sampleDialog"
      >
        <el-button
          type="default"
          plain
          @click="back"
        >Cancel</el-button>
        <el-button
          type="default"
          plain
          @click="save"
        >Skip And Submit</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject } from 'vue-property-decorator';
import RegisterStepOne from './sub-view/register-step-one.vue';
import RegisterStepTwo from './sub-view/register-step-two.vue';
import RegisterStepThree from './sub-view/register-step-three.vue';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import MetadataService from '@/services/Metadata.service';
import Util from '@/services/Util.service';
import moment from 'moment';
import _ from 'lodash';
import * as Logger from '@/plugins/logs/zds-logger';

@Component({
  components: {
    RegisterStepOne,
    RegisterStepTwo,
    RegisterStepThree
  }
})
export default class MetadataRegisterVDM extends Vue {
  @Inject('metadataService')
  metadataService: MetadataService;

  @Inject('doeRemoteService')
  doeRemoteService: DoeRemoteService;

  active = 0;
  loading = false;
  dialogVisible = false;
  sampleQueryDialogVisible = false;

  created() {
    this.clearStore();
  }

  lastStep() {
    if (this.active > 0) this.active -= 1;
  }

  nextStep() {
    if (this.active == 0) {
      const $one: any = this.$refs.one;
      if ($one && $one.doNext) {
        $one.doNext();
      }
    }else if (this.active == 1) {
      const $two: any = this.$refs.two;
      if ($two && $two.doNext) {
        $two.doNext();
      }
    }else if (this.active < 2) this.active += 1;
  }

  add() {
    this.dialogVisible = false;
    this.clearStore();
    this.active = 0;
  }

  clearStore() {
    this.$store.dispatch('setRegisterStepOne', {});
    this.$store.dispatch('setRegisterStepTwo', []);
    this.$store.dispatch('setRegisterStepThree', []);
    this.$store.dispatch('setUpdateColumns', []);
  }

  view() {
    this.dialogVisible = false;
    const store_one: any = this.$store.getters.getRegisterStepOne;
    sessionStorage.setItem('metadata_table', _.toUpper(store_one.database) + '.' +  _.toUpper(store_one.table_name.trim()));
    sessionStorage.setItem('metadata_table_type', 'vdm');
    this.$router.push('/metadata');
  }

  save() {
    const $three: any = this.$refs.three;
    const $edit: any = $three.$refs.edit;
    if ($edit && $edit.add) {
      let valid = false;
      ($edit.$refs['form'] as any).validate((valid_: boolean) => (valid = valid_));
      if (!valid) {
        this.sampleQueryDialogVisible = false;
        return;
      }
      $edit.add();
    }
    this.submit();
  }

  back() {
    this.sampleQueryDialogVisible = false;
  }

  preSubmit() {
    const $three: any = this.$refs.three;
    if ($three && $three.$refs.edit && $three.$refs.edit.form && (!_.isEmpty($three.$refs.edit.form.title) || !_.isEmpty($three.$refs.edit.form.query_text))) {
      this.sampleQueryDialogVisible = true;
    } else {
      this.submit();
    }
  }

  submit() {
    Logger.counter('REGISTER_DATASET', 1, { name: 'submit' });
    this.sampleQueryDialogVisible = false;
    this.registerVDM();
    this.addColDesc();
    this.addSampleQuery();
  }

  async registerVDM() {
    const store_one: any = this.$store.getters.getRegisterStepOne;
    if (!_.isEmpty(store_one.database) && !_.isEmpty(store_one.table_name.trim())) {
      const params: any = {
        // owner_name: store_one.owner,
        // owner_nt: store_one.ownerNt,
        ownerList: store_one.ownerList,
        team: store_one.team,
        team_dl: store_one.dl,
        platforms: _.join(store_one.platform, ','),
        db: _.toLower(store_one.database),
        table: _.toLower(store_one.table_name.trim()),
        git_link: store_one.github,
        script: store_one.script,
        table_desc: store_one.description,
        cre_user: Util.getNt()
      };
      this.loading = true;
      this.doeRemoteService.registerVDM(params).then((res: any) => {
        console.log('Call Api:registerVDM success');
        if (res && res.status == 200) {
          this.dialogVisible = true;
          this.putESTable();
        }else {
          this.$message.error('Register failed: ' + JSON.stringify(res));
        }
        this.loading = false;
      }, function(error: any) {
        if (error.message == 'Unknown error') error.message = 'Register failed';
        throw error;
      }).catch(err => {
        console.log('Call Api:registerVDM failed: ' + JSON.stringify(err));
        this.loading = false;
      });
    }
  }

  async addColDesc() {
    const now: string = moment().utcOffset('-07:00').format('YYYY-MM-DD HH:mm:ss');
    const store_one: any = this.$store.getters.getRegisterStepOne;
    const columns: Array<any> = [];
    if (!_.isEmpty(store_one.database) && !_.isEmpty(store_one.table_name.trim())) {
      _.forEach(this.$store.getters.getUpdateColumns, (v: any) => {
        if (!_.isUndefined(v.column_desc)) {
          const row: any = {
            table:  _.toLower(store_one.database) + '.' +  _.toLower(store_one.table_name.trim()),
            name:  _.toUpper(v.column_name),
            upd_date: now,
            upd_user: Util.getNt(),
            desc: v.column_desc.replace(/<[^>]*>|/g,''),
            desc_html: v.column_desc
          };
          columns.push(row);
        }
      });

      if (!_.isEmpty(columns)) {
        const params: any = {columns: columns};
        this.doeRemoteService.addColDesc(params).then((res: any) => {
          console.log('Call Api:addColDesc success');
          if (res && res.status == 200) {
            console.log('addColDesc success');
          }else {
            console.log('addColDesc failed: ' + JSON.stringify(res));
          }
        }).catch(err => {
          console.log('Call Api:addColDesc failed: ' + JSON.stringify(err));
        });
      }
    }
  }

  async addSampleQuery() {
    const store_one: any = this.$store.getters.getRegisterStepOne;
    const store_three: any = this.$store.getters.getRegisterStepThree;
    if (!_.isEmpty(store_one.database) && !_.isEmpty(store_one.table_name.trim())) {
      _.forEach(store_three, (v: any) => {
        v.table = _.toLower(store_one.database) + '.' +  _.toLower(store_one.table_name.trim());
        this.doeRemoteService.addSampleQuery(v).then((res: any) => {
          console.log('Call Api:addSampleQuery success');
          if (res && res.status == 200) {
            console.log('addSampleQuery success');
          }else {
            console.log('addSampleQuery failed: ' + JSON.stringify(res));
          }
        }).catch(err => {
          console.log('Call Api:addSampleQuery failed: ' + JSON.stringify(err));
        });
      });
    }
  }

  async putESTable() {
    const store_one: any = this.$store.getters.getRegisterStepOne;
    if (!_.isEmpty(store_one.database) && !_.isEmpty(store_one.table_name.trim())) {
      const params: any = {
        obj: "table",
        id: _.toLower(store_one.database) + '.' + _.toLower(store_one.table_name.trim())
      }
      const data: any = {
        name: _.toLower(store_one.database) + '.' + _.toLower(store_one.table_name.trim()),
        row_type: 'vdm',
        platform: _.join(store_one.platform, ','),
        desc: store_one.description,
        total_score: 0,
        "normal name": _.union(_.split(_.toLower(store_one.database), "_"), _.split(_.toLower(store_one.table_name), "_")),
        "tags": _.union(_.split(_.toLower(store_one.database), "_"), _.split(_.toLower(store_one.table_name), "_"))
      };

      this.metadataService.putESTable(params, data).then(metas => {
        console.log('update es success');
        this.$store.dispatch('setMetadataReload', true);
      }).catch(err => {
        console.log('update es failed: ' + JSON.stringify(err));
      });
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
.register {
  height: 100%;
}
.steps-bar {
  display: flex;
  flex-direction: row;
  height: 46px;
  .el-steps {
    width: 100%;
    height: 20px;
  }
  .el-step.is-simple {
    /deep/ .el-step__icon.is-text {
      border: 1px solid;
    }
    /deep/ .el-step__title {
      font-size: 14px;
    }
    /deep/ .el-step__head.is-wait {
      .el-step__icon.is-text {
        border-color: #999999;
      }
    }
    /deep/ .el-step__title.is-wait {
      color: #999999;
    }
    /deep/ .el-step__head.is-process {
      .el-step__icon.is-text {
        border-color: #569CE1;
      }
    }
    /deep/ .el-step__title.is-process {
      font-weight: 400;
      color: #569CE1;
    }
    /deep/ .el-step__head.is-success {
      .el-step__icon.is-text {
        border-color: #5CB85C;
      }
    }
    /deep/ .el-step__title.is-success {
      color: #5CB85C;
    }
  }
}
.main-content {
  height: calc( 100% - 46px - 70px );
}
.button-group {
  bottom: 0;
  padding: 20px 0;
  padding-left: 40%;
  position: fixed;
  .blank-div {
    display: inline-block;
    width: 100px;
  }
}
.dialog-header {
  [class^="zeta-icon-"],
  [class*=" zeta-icon-"] {
    font-size: 20px;
  }
  .zeta-icon-finish {
    color: #5CB85C;
  }
  .zeta-icon-info {
    color: #F3AF2B;
  }
  > span {
    font-size: 18px;
    font-weight: 700;
    padding-left: 5px;
    line-height: 20px;
  }
  .el-icon-close {
    font-size: 18px;
  }
}
.sampleDialog {
  display: flex;
  flex-direction: row-reverse;
  justify-content: center;
}
</style>