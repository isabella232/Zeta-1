<template>
  <div
    id="overview"
    class="overview"
  >
    <header class="sticky-header">
      Overview
    </header>
    <labeled>
      <label slot="label">Description</label>
      <div v-html="table.table_desc" />
    </labeled>
    <labeled>
      <label slot="label">Subject Area</label>
      <a style="cursor:pointer;" @click="() => $router.push(`/metadata/sa/${table.subject_area}`)">{{ table.subject_area }}</a>
    </labeled>
    <labeled>
      <label slot="label">
        Contact 
        <a :href="emailUrl">
          <i class="el-icon-message" />
        </a>
      </label>
      <user-selection
        :nt="table.product_owner"
        disabled
      />
      <el-tooltip content="Product Owner">
        <span class="position">
        (PO)
        </span>
      </el-tooltip>
      <slack-invoker :nt="table.product_owner">
        <span class="slack">
          <img :src="slackIconUrl"> 
        </span>
      </slack-invoker>
      <user-selection 
        :nt="table.dev_manager"
        disabled
      />
      <el-tooltip content="Dev Manager">
        <span class="position">
        (DM)
        </span>
      </el-tooltip>
      <slack-invoker :nt="table.dev_manager">
        <span class="slack">
          <img :src="slackIconUrl"> 
        </span>
      </slack-invoker>
      <user-selection 
        :nt="table.primary_bsa_sae"
        disabled
      />
      <el-tooltip content="Subject Area Expert">
        <span class="position">
          (SAE)
        </span>
      </el-tooltip>
      <slack-invoker :nt="table.primary_bsa_sae">
        <span class="slack">
          <img :src="slackIconUrl"> 
        </span>
      </slack-invoker>
      <user-selection 
        :nt="table.primary_dev_sae"
        disabled
      />
      <el-tooltip content="Subject Area Expert">
        <span class="position">
          (SAE)
        </span>
      </el-tooltip>
      <slack-invoker :nt="table.primary_dev_sae">
        <span class="slack">
          <img :src="slackIconUrl"> 
        </span> 
      </slack-invoker>
    </labeled>
    <labeled>
      <label slot="label">Labels</label>
      <el-tag
        v-for="l in labels"
        :key="l"
        size="mini"
        style="margin-right:8px;"
      >
        {{ l }}
      </el-tag>
    </labeled>
    <labeled>
      <label slot="label">30-Day Usage</label>
      <div>
        {{ usage }}
        <i
          class="zeta-icon-download"
          style="cursor:pointer;"
          @click="downloadUsage"
        />
      </div>
    </labeled>
  </div>
</template>
<script lang="ts">
import { Component, Mixins } from 'vue-property-decorator';
import { Getter, State } from 'vuex-class';
import { Getters } from './store';
import Labeled from '@/components/AutoEL/Create/components/Labeled.vue';
import UserSelection from '@/components/common/UserSelection.vue';
import SlackInvoker from '@/components/common/SlackInvoker.vue';
import { attempt } from '@drewxiu/utils';
import moment from 'moment';
import { downloadUsage } from '@/components/Metadata/DownloadUsage.ts';
import { Common } from './mixins/Common';

@Component({
  components: {
    Labeled,
    UserSelection,
    SlackInvoker,
  },
})
export default class Overview extends Mixins(Common) {
  @Getter(Getters.TableSample) tableSample;
  @State(state => state.TableDetail.labels) labels;

  get table () {
    return this.tableSample || {};
  }
  get usage () {
    return attempt(() => this.table.all_distinct_batch_cnt + this.table.all_distinct_user_cnt, 0);
  }
  get emailUrl () {
    return  `mailto:${[this.table.product_owner, this.table.dev_manager, this.table.primary_bsa_sae, this.table.primary_dev_sae].join('@ebay.com,') + '@ebay.com'}`;
  }
  get slackIconUrl () {
    return require('../../../../public/img/icons/slack.svg');
  }
  downloadUsage () {
    const params: any = {
      table: this.table.table_name,
      startDate: moment()
        .subtract(30, 'days')
        .format('YYYY-MM-DD'),
      endDate: moment().format('YYYY-MM-DD'),
    };
    downloadUsage(params);
  }
}
</script>
<style lang="scss" scoped>

.overview {
  padding: 0 12px 12px;
  label {
    font-weight: bold;
    text-align: left;
    padding-left: 40px;
    padding-right: 12px;
  }
  .position {
    display: inline-block;
  }
  .slack {
    margin-right: 30px;
    vertical-align: middle;
    cursor: pointer;
  }
}
</style>