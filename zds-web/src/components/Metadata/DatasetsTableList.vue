<template>
  <div
    v-loading="loading"
    class="tb-list-div"
  >
    <div  
      v-if="readOnly"
      class="readonly-popup"
    >
      <span class="left info">
        <i class="zeta-icon-info" />
        <template> This page is READ ONLY</template>
      </span>
    </div>
    <div style="height: 100%;">
      <Header
        :show-back-button="true"
        :breadcrumb="[{ key: 'Metadata', label: 'Metadata' }, { key: 'DataSets', label: data.domain }]"
        @breadcrumb-click="backTo"
      />
      <el-row class="tb-title">
        <p>
          <span
            class="btn-label"
            @click="back"
          >{{ data.domain }}</span>
          <ShareLink
            v-if="!readOnly"
            v-model="shareUrl"
          />
        </p>
      </el-row>
      <el-row class="tb-domain">
        <el-radio-group
          v-model="selectSub"
          size="small"
        >
          <el-radio-button
            v-click-metric:METADATA_BROWSE="{name: activeTab.toLowerCase(), domain: data.domain}"
            label="All"
            border
          >
            All
          </el-radio-button>
          <el-radio-button
            v-for="sub in data.subDomain"
            :key="sub"
            v-click-metric:METADATA_BROWSE="{name: activeTab.toLowerCase(), trigger: (readOnly ? 'share' : 'zeta'), domain: data.domain, subdomain: sub}"
            :label="sub"
            border
          >
            {{ sub }}
          </el-radio-button>
        </el-radio-group>
      </el-row>
      <div class="metadata-tools-bar">
        <ul class="metadata-tabs">
          <li
            v-click-metric:METADATA_BROWSE="{name: 'overview', trigger: (readOnly ? 'share' : 'zeta'), domain: data.domain, subdomain: (subDomain != '' ? subDomain : undefined)}"
            :class="{'active':activeTab ==='Overview'}"
            @click="()=> {activeTab = 'Overview';initOverviewScroll();}"
          >
            Overview
          </li>
          <li
            v-click-metric:METADATA_BROWSE="{name: 'dataset', trigger: (readOnly ? 'share' : 'zeta'), domain: data.domain, subdomain: (subDomain != '' ? subDomain : undefined)}"
            :class="{'active':activeTab ==='Dataset'}"
            @click="()=> activeTab = 'Dataset'"
          >
            Dataset
          </li>
        </ul>
        <div class="btn-group">
          <template v-if="activeTab == 'Overview' && isOwner && editDisabled && !readOnly">
            <el-button
              type="primary"
              class="tab-btn"
              @click="editOverview()"
            >
              Edit
            </el-button>
          </template>
          <template v-if="activeTab == 'Overview' && isOwner && !editDisabled && !readOnly">
            <el-button
              type="primary"
              class="tab-btn"
              @click="saveOverview()"
            >
              Save
            </el-button>
            <el-button
              type="default"
              plain
              class="tab-btn"
              @click="revertOverview()"
            >
              Cancel
            </el-button>
          </template>
        </div>
      </div>
      <div
        v-loading="loading"
        class="metadata-display"
      >
        <div
          v-show="activeTab ==='Overview'"
          class="overview"
        >
          <DatasetOverviewPanel
            ref="overview"
            :data="qaData"
            :desc="desc"
            :reference="refArr"
            :domain="data.domain"
            :sub-domain="subDomain"
            :new-arr="newArr"
            :owner="ownerArr"
            :edit-disabled="editDisabled"
            :customer-panel="customerArr"
            :comments="comments"
            :is-owner="isOwner"
            @update-desc="updateDesc"
            @update-comment="updateDomainComment"
            @delete-comment="deleteDomainComment"
            @add-reply="addDomainComment"
            @update-reply="updateDomainReply"
            @delete-reply="deleteReplyComments"
            @submit-conf="updateDomainConf"
            @send-email="sendMail"
          />
        </div>
        <div
          v-show="activeTab ==='Dataset'"
          class="Dataset"
        >
          <DatasetTablePanel
            :data="originData"
            :is-owner="isOwner && !readOnly"
            :new-arr="newArr"
            :owner="ownerArr"
            :top="topArr"
            :reference="refArr"
            :origin="data"
            :domain="data.domain"
            :sub-domain="subDomain"
            @submit-conf="updateDomainConf"
            @send-email="sendMail"
            @refresh-enotify="getEnotifyByDomain"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Provide, Watch } from 'vue-property-decorator';
import ShareLink from '@/components/common/share-link';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import UserInfoRemoteService from '@/services/remote/UserInfo';
import moment from 'moment';
import _ from 'lodash';
import Util from '@/services/Util.service';
import DatasetTablePanel from './DatasetTablePanel.vue';
import DatasetOverviewPanel from './DatasetOverviewPanel.vue';
import { ZetaExceptionProps } from '@/types/exception';
import Header from '@/components/Metadata/index/components/Header.vue';

@Component({
  components: {
    DatasetTablePanel,
    DatasetOverviewPanel,
    ShareLink,
    Header,
  },
})
export default class DatasetsTableList extends Vue {
  @Provide() doeRemoteService: DoeRemoteService = new DoeRemoteService();
  @Provide()
  userInfoRemoteService: UserInfoRemoteService = new UserInfoRemoteService();

  data: any = {};
  readOnly = false;
  domain = null;
  desc = '';
  loading = false;
  originData: any = [];
  allTableData: any = [];
  selectSub = 'All';
  activeTab = 'Dataset';
  topArr: any = [];
  ownerArr: any = [];
  refArr: any = [];
  newArr: any = [];
  comments: any = [];
  domainConf: any = {};
  fullName: any = '';
  qaData: any = '';
  editDisabled = true;
  subDomain: any = '';
  currentNt: any = Util.getNt();
  customerArr: any = [];

  get isOwner (): boolean {
    return (
      _.findIndex(this.ownerArr, (v: any) => {
        return v.nt == Util.getNt();
      }) > -1
    );
  }

  get shareUrl () {
    return (
      `${location.protocol}//${
        location.host
      }/${Util.getPath()}/#/metadata/datasets?domain=${
        this.data.domain
      }` + (this.subDomain ? `&subdomain=${this.subDomain}` : '')
    );
  }

  created () {
    const { domain, subdomain, readOnly } = this.$route.query as any;
    if (!domain) {
      this.back();
      return;
    }
    this.subDomain = this.selectSub = subdomain || 'All';
    this.doeRemoteService.getDomainInfo().then(({ data }) => {
      const domains = data.data.value;
      const domainInfo = domains.find(d => d.domain === domain);
      if (!domainInfo) {
        this.back();
        return;
      }
      this.domain = domainInfo;
      this.data = {
        name: this.subDomain || domainInfo.domain,
        domain: domainInfo.domain,
        isDomain: !!this.subDomain,
        subDomain: (domainInfo.sub_domain || '').split(',').filter(Boolean),
      };
    });
    this.fullNameByNt();
    this.readOnly =
      readOnly === 'true' || window.location.href.indexOf('/share/') > -1;
    const props: ZetaExceptionProps = {
      path: 'metadata',
    };
    this.doeRemoteService.props(props);
    this.userInfoRemoteService.props(props);
  }

  openNewTab () {
    window.open(this.shareUrl, this.data.domain + this.subDomain);
  }

  backTo () {
    this.$router.push('/metadata');
  }

  @Watch('selectSub')
  @Watch('domain')
  getDataSet () {
    if (!this.domain) return;
    let params: any = {};
    if (this.selectSub == 'All') {
      params = {
        domain: this.data.domain,
        name: this.data.domain,
        isDomain: true,
      };
      this.subDomain = '';
      this.getTableListByDomain(this.data.domain);
      this.getEnotifyByDomain({ domain: this.data.domain, limit: 10 });
      this.getDomainDesc({ domain: this.data.domain, isDomain: true });
    } else {
      params = {
        domain: this.data.domain,
        sub_domain: this.selectSub,
        name: this.selectSub,
        isDomain: false,
      };
      this.subDomain = this.selectSub;
      this.getTableListBySubDomain(this.selectSub);
      this.getEnotifyByDomain({ sub_domain: this.selectSub, limit: 10 });
      this.getDomainDesc({
        domain: this.data.domain,
        sub_domain: this.selectSub,
        isDomain: false,
      });
    }
    this.$emit('refresh-desc', params);
    this.getDomainConf(params);
    this.getDomainComment();
  }

  getEnotifyByDomain (params: any) {
    this.newArr = [];
    this.doeRemoteService.getEnotifyByDomain(params).then((res: any) => {
      if (res && res.data && res.data.data && res.data.data.value) {
        this.newArr = res.data.data.value;
      }
    });
  }

  getTableListByDomain (name: string) {
    const params = { domain: name };
    this.loading = true;
    this.doeRemoteService
      .getTableListByDomain(params)
      .then(res => {
        if (res && res.data && res.data.data && res.data.data.value) {
          this.originData = res.data.data.value;
          this.allTableData = res.data.data.value;
        }
        this.loading = false;
      })
      .catch(err => {
        this.loading = false;
      });
  }

  getDomainConf (params: any) {
    this.doeRemoteService
      .getDomainConf(params)
      .then(res => {
        if (res && res.data && res.data.data && res.data.data.value) {
          this.domainConf = res.data.data.value[0] || {};
          this.topArr = JSON.parse(this.domainConf.top_datasets) || [];
          this.ownerArr = JSON.parse(this.domainConf.owner) || [];
          this.refArr = JSON.parse(this.domainConf.reference) || [];
          this.qaData = this.domainConf.tabs || '';
          this.customerArr = JSON.parse(this.domainConf.sections) || [];
        }
        this.domainConf.domain = params.domain;
        this.domainConf.sub_domain = params.sub_domain;
      })
      .catch(err => {
        this.domainConf.domain = params.domain;
        this.domainConf.sub_domain = params.sub_domain;
      });
  }

  getDomainDesc (params: any) {
    this.desc = '';
    this.doeRemoteService.getDomainDesc(params).then(res => {
      if (
        res &&
        res.data &&
        res.data.data &&
        res.data.data.value &&
        res.data.data.value[0]
      ) {
        this.desc =
          (params.isDomain
            ? res.data.data.value[0]['domain_desc']
            : res.data.data.value[0]['sub_domain_desc']) || '';
      }
    });
  }

  updateDomainConf (conf: any) {
    const params: any = _.cloneDeep(this.domainConf) || {};
    params.sub_domain = params.sub_domain ? params.sub_domain : '';
    if (conf.owner) params.owner = conf.owner;
    if (conf.reference) params.reference = conf.reference;
    if (conf.top_datasets) params.top_datasets = conf.top_datasets;
    if (conf.tabs) params.tabs = conf.tabs;
    if (conf.sections) params.sections = conf.sections;
    params.cre_user = params.cre_user ? params.cre_user : Util.getNt();
    params.cre_date = params.cre_date ? params.cre_date : undefined;
    params.upd_user = Util.getNt();
    this.doeRemoteService
      .updateDomainConf(params)
      .then(
        (res: any) => {
          console.debug('Call Api:updateDomainConf success');
          if (res && res.status == 200) {
            //this.$message.success("Submit Success");
            if (conf.top_datasets)
              this.domainConf.top_datasets = conf.top_datasets;
            if (conf.owner) this.domainConf.owner = conf.owner;
            if (conf.reference) this.domainConf.reference = conf.reference;
          }
        },
        function (error: any) {
          if (error.message == 'Unknown error')
            error.message = 'Submit config error';
          throw error;
        }
      )
      .catch(err => {
        this.getDomainConf(params);
        console.error(
          'Call Api:updateDomainConf failed: ' + JSON.stringify(err)
        );
      });
  }

  updateDesc (desc: any) {
    const params: any = {
      desc: desc,
      domain: this.data.domain,
      sub_domain: this.subDomain,
      isSubdomain: this.subDomain == '' ? false : true,
    };

    if (params.isSubdomain) {
      this.doeRemoteService
        .updateSubdomainDesc(params)
        .then(
          (res: any) => {
            console.debug('Call Api:updateSubdomainDesc success');
            if (res && res.status == 200) {
              const child: any = this.$refs.overview;
              if (child && child.closeDescEdit) child.closeDescEdit();
            }
          },
          function (error: any) {
            if (error.message == 'Unknown error')
              error.message = 'Update description error';
            throw error;
          }
        )
        .catch(err => {
          this.getDomainConf(params);
          console.error(
            'Call Api:updateSubdomainDesc failed: ' + JSON.stringify(err)
          );
        });
    } else {
      this.doeRemoteService
        .updateDomainDesc(params)
        .then(
          (res: any) => {
            console.debug('Call Api:updateDomainDesc success');
            if (res && res.status == 200) {
              const child: any = this.$refs.overview;
              if (child && child.closeDescEdit) child.closeDescEdit();
            }
          },
          function (error: any) {
            if (error.message == 'Unknown error')
              error.message = 'Update description error';
            throw error;
          }
        )
        .catch(err => {
          this.getDomainConf(params);
          console.error(
            'Call Api:updateDomainDesc failed: ' + JSON.stringify(err)
          );
        });
    }
  }

  getTableListBySubDomain (name: string) {
    const params = { sub_domain: name };
    this.loading = true;
    this.doeRemoteService
      .getTableListBySubDomain(params)
      .then(res => {
        if (res && res.data && res.data.data && res.data.data.value) {
          this.originData = res.data.data.value;
          this.allTableData = res.data.data.value;
        }
        this.loading = false;
      })
      .catch(err => {
        this.loading = false;
      });
  }

  back () {
    this.$router.push('/metadata');
  }

  getDomainComment () {
    let params: any = {};
    if (this.data.isDomain && this.data.name && this.selectSub == 'All') {
      params = { domain_name: this.data.name };
    } else if (this.data.isDomain && this.selectSub != 'All') {
      params = { subdomain_name: this.selectSub };
    } else if (this.data.name) {
      params = { subdomain_name: this.data.name };
    }
    if (this.selectSub == 'All') {
      params = { domain_name: this.data.domain };
    } else {
      params = { subdomain_name: this.selectSub };
    }
    this.doeRemoteService
      .getDomainComments(params)
      .then(
        (res: any) => {
          console.debug('Call Api:getDomainComments success');
          if (res && res.data && res.data != null) {
            if (
              !_.isUndefined(res.data.data) &&
              res.data.data.hasOwnProperty('value')
            ) {
              this.comments = res.data.data.value;
            } else {
              this.comments = [];
            }
          } else {
            this.comments = [];
          }
        },
        function (error: any) {
          error.resolve();
          throw error;
        }
      )
      .catch(err => {
        console.error(
          'Call Api:getDomainComments failed: ' + JSON.stringify(err)
        );
        this.comments = [];
      });
  }

  addDomainComment (params: any) {
    if (params) {
      const now: string = moment()
        .utcOffset('-07:00')
        .format('YYYY-MM-DD HH:mm:ss');
      params.cre_date = now;
      params.cre_user = Util.getNt();
      params.cre_user_name = this.fullName;
      if (this.data.isDomain && this.data.name && this.selectSub == 'All') {
        params.domain_name = this.data.name;
      } else if (this.data.isDomain && this.selectSub != 'All') {
        params.domain_name = this.data.domain;
        params.subdomain_name = this.selectSub;
      } else if (this.data.name) {
        params.domain_name = this.data.domain;
        params.subdomain_name = this.data.name;
      }
      this.doeRemoteService
        .addDomainComments(params)
        .then(
          (res: any) => {
            console.debug('Call Api:addDomainComments success');
            if (res && res.status == 200) {
              this.getDomainComment();
              if (params.reply_id) {
                const child: any = this.$refs.overview;
                if (child && child.cancelNewReply) child.cancelNewReply();
              } else {
                const child: any = this.$refs.overview;
                if (child && child.cancelNewComment) child.cancelNewComment();
              }
              this.$message.success('Submit Success');
            }
          },
          function (error: any) {
            if (error.message == 'Unknown error')
              error.message = 'Add comment error';
            throw error;
          }
        )
        .catch(err => {
          console.error(
            'Call Api:addDomainComments failed: ' + JSON.stringify(err)
          );
        });
    } else {
      this.$message.info('No comment submit.');
    }
  }

  deleteDomainComment (params: any) {
    if (params) {
      this.doeRemoteService
        .deleteDomainComments(params)
        .then(
          (res: any) => {
            console.debug('Call Api:deleteDomainComment success');
            this.getDomainComment();
            if (res && res.status == 200) {
              this.$message.success('Submit Success');
            }
          },
          function (error: any) {
            if (error.message == 'Unknown error')
              error.message = 'Delete comment error';
            throw error;
          }
        )
        .catch(err => {
          console.error(
            'Call Api:deleteDomainComment failed: ' + JSON.stringify(err)
          );
        });
    } else {
      this.$message.info('No comment delete.');
    }
  }

  deleteReplyComments (params: any) {
    if (params) {
      this.doeRemoteService
        .deleteReplyComments(params)
        .then(
          (res: any) => {
            console.debug('Call Api:deleteReplyComments success');
            this.getDomainComment();
            if (res && res.status == 200) {
              this.$message.success('Submit Success');
            }
          },
          function (error: any) {
            if (error.message == 'Unknown error')
              error.message = 'Delete comment error';
            throw error;
          }
        )
        .catch(err => {
          console.error(
            'Call Api:deleteReplyComments failed: ' + JSON.stringify(err)
          );
        });
    } else {
      this.$message.info('No comment delete.');
    }
  }

  updateDomainComment (params: any) {
    if (params) {
      this.doeRemoteService
        .updateDomainComment(params)
        .then(
          (res: any) => {
            console.debug('Call Api:updateDomainComment success');
            if (res && res.status == 200) {
              const child: any = this.$refs.overview;
              if (child && child.clearCommentEdit) child.clearCommentEdit();
              this.$message.success('Submit Success');
            }
          },
          function (error: any) {
            if (error.message == 'Unknown error')
              error.message = 'Update comment error';
            throw error;
          }
        )
        .catch(err => {
          console.error(
            'Call Api:updateDomainComment failed: ' + JSON.stringify(err)
          );
        });
    } else {
      this.$message.info('No comment delete.');
    }
  }

  updateDomainReply (params: any) {
    if (params) {
      this.doeRemoteService
        .updateDomainReply(params)
        .then(
          (res: any) => {
            console.debug('Call Api:updateDomainReply success');
            if (res && res.status == 200) {
              const child: any = this.$refs.overview;
              if (child && child.clearReplyEdit) child.clearReplyEdit();
              this.$message.success('Submit Success');
            }
          },
          function (error: any) {
            if (error.message == 'Unknown error')
              error.message = 'Update comment error';
            throw error;
          }
        )
        .catch(err => {
          console.error(
            'Call Api:updateDomainReply failed: ' + JSON.stringify(err)
          );
        });
    } else {
      this.$message.info('No comment delete.');
    }
  }

  fullNameByNt () {
    this.fullName = undefined;
    this.userInfoRemoteService
      .getBaseInfo(Util.getNt())
      .then((res: any) => {
        this.fullName =
          res.data.response.docs[0].last_name +
          ', ' +
          res.data.response.docs[0].first_name;
      })
      .catch(e => {
        console.error('get full name error', e);
      });
  }

  editOverview () {
    this.editDisabled = false;
  }

  revertOverview () {
    const overview: any = this.$refs.overview;
    if (overview) {
      overview.descModel = this.desc;
      overview.commonModel = this.qaData;
      this.editDisabled = true;
    }
  }

  saveOverview () {
    const overview: any = this.$refs.overview;
    if (overview) {
      if (overview.updateDesc) {
        overview.updateDesc();
        this.desc = overview.descModel;
      }

      if (overview.save) {
        overview.save();
        this.qaData = overview.commonModel;
      }
      this.editDisabled = true;
    }
  }

  async sendMail (param: any) {
    let content: any = {};
    let subject = '';
    const link: string =
      `${location.protocol}//${
        location.host
      }/${Util.getPath()}#/metadata/datasets?` +
      'domain=' +
      this.data.domain +
      (this.subDomain != '' ? '&subdomain=' + this.subDomain : '');
    if (!(param && param.action)) {
      return;
    }
    if (param.action == 'addowner') {
      subject = this.data.name + ' - add new owner';
      content = {
        name: param.to,
        msg:
          'You are added into owner list of ' +
          (this.subDomain == ''
            ? 'domain ' + this.data.domain
            : 'subdomain ' + this.subDomain) +
          ' by ' +
          this.fullName +
          '.<br/><a href=' +
          link +
          '>',
      };
    } else if (param.action == 'removeowner') {
      subject = this.data.name + ' - delete owner';
      content = {
        name: param.to,
        msg:
          'You are removed into owner list of ' +
          (this.subDomain == ''
            ? 'domain ' + this.data.domain
            : 'subdomain ' + this.subDomain) +
          ' by ' +
          this.fullName +
          '.<br/><a href=' +
          link +
          '>',
      };
    } else if (param.action == 'addcomment') {
      subject = this.data.name + ' - add comment';
      content = {
        name: param.to,
        msg:
          this.fullName +
          ' added a new comment of ' +
          this.data.name +
          ' in the Metadata browse.<br/><br/>' +
          param.content +
          '<br/><a href=' +
          link +
          '>',
      };
    } else if (param.action == 'updatecomment') {
      subject = this.data.name + ' - update comment';
      content = {
        name: param.to,
        msg:
          this.fullName +
          ' updated a comment of ' +
          this.data.name +
          ' in the Metadata browse.<br/><br/>' +
          param.content +
          '<br/><a href=' +
          link +
          '>',
      };
    } else if (param.action == 'removecomment') {
      subject = this.data.name + ' - delete comment';
      content = {
        name: param.to,
        msg:
          this.fullName +
          ' deleted a comment of ' +
          this.data.name +
          ' in the Metadata browse.<br/><br/>' +
          param.content +
          '<br/><a href=' +
          link +
          '>',
      };
    } else if (param.action == 'addreply') {
      subject = this.data.name + ' - add reply';
      content = {
        name: param.to,
        msg:
          this.fullName +
          ' added a new reply of ' +
          this.data.name +
          ' in the Metadata browse.<br/><br/>' +
          param.content +
          '<br/><a href=' +
          link +
          '>',
      };
    } else if (param.action == 'updatereply') {
      subject = this.data.name + ' - update reply';
      content = {
        name: param.to,
        msg:
          this.fullName +
          ' updated a reply of ' +
          this.data.name +
          ' in the Metadata browse.<br/><br/>' +
          param.content +
          '<br/><a href=' +
          link +
          '>',
      };
    } else if (param.action == 'removereply') {
      subject = this.data.name + ' - delete reply';
      content = {
        name: param.to,
        msg:
          this.fullName +
          ' deleted a reply of ' +
          this.data.name +
          ' in the Metadata browse.<br/><br/>' +
          param.content +
          '<br/><a href=' +
          link +
          '>',
      };
    } else {
      return;
    }

    const params: any = {
      fromAddr: 'DL-eBay-Metadata@ebay.com',
      toAddr: '',
      subject: subject,
      content: JSON.stringify(content),
      template: 'ZetaNotification',
      ccAddr: '',
      type: 3, //1: html; 2: txt
    };
    let nts: any = param.nts;
    nts.push(Util.getNt());
    nts = _.uniq(nts);
    for await (const nt of nts) {
      if (!_.isEmpty(nt)) {
        const res = await this.doeRemoteService.getEmailAddr(nt);
        if (
          res &&
          res.data &&
          res.data != null &&
          !_.isEmpty(res.data.data) &&
          !_.isEmpty(res.data.data.value) &&
          res.data.data.value.length > 0
        ) {
          _.forEach(res.data.data.value, (v: any) => {
            params.toAddr = params.toAddr + v.mail + ';';
          });
        }
      }
    }
    if (!_.isEmpty(params.toAddr)) {
      this.doeRemoteService
        .createEmail(params)
        .then(res => {
          console.debug('Call Api:createEmail successed');
        })
        .catch(err => {
          console.error('Call Api:createEmail failed: ' + JSON.stringify(err));
        });
    }
  }

  initOverviewScroll () {
    const overview: any = this.$refs.overview;
    if (overview && overview.activities) {
      overview.scrollTo(overview.activities[0].content);
    }
  }

  @Watch('selectSub')
  onSelectSub () {
    this.revertOverview();
    this.getDataSet();
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.tb-list-div {
  padding: 8px 16px 0;
  box-sizing: border-box;
}
.el-row {
  margin-bottom: 15px;
}
.tb-list-div {
  height: 100%;
  width: 100%;
  overflow-y: hidden;
  position: relative;
}
.tb-title {
  padding-top: 8px;
}
.btn-label {
  font-size: 18px;
  font-style: normal;
  font-weight: 700;
  display: inline-block;
  padding-top:8px;
  margin-right: 10px;
}
.metadata-tools-bar {
  align-items: center;
  display: flex;
  height: 40px;
  justify-content: space-between;
  ul.metadata-tabs {
    display: flex;
    height: 100%;
    list-style-type: none;
    > li {
      align-items: center;
      border-bottom: 1px solid #cacbcf;
      color: #999;
      cursor: pointer;
      display: flex;
      font-size: 14px;
      padding: 0 20px;
      &.active {
        border-bottom: 1px solid #569ce1;
        background-color: #fff;
        color: $zeta-global-color;
      }
      &:hover {
        color: #569ce1;
      }
    }
  }
  .btn-group {
    border-bottom: 1px solid #cacbcf;
    display: inline-block;
    height: 39px;
    padding: 0 10px;
    text-align: right;
    width: 100%;
  }
}
.metadata-display {
  display: block;
  height: calc(100% - 185px);
  > div {
    height: 100%;
    padding-top: 10px;
  }
  .overview {
    overflow-y: hidden;
  }
  .comment-div {
    height: calc(100% - 255px);
    width: 100%;
    overflow-y: auto;
  }
  .qa {
    height: 100%;
  }
}
.el-icon-back {
  cursor: pointer;
  font-size: 18px;
  float: left;
  color: #569ce1;
}
.tb-domain {
  margin-bottom: 10px;
}
.el-radio-button {
  box-shadow: none !important;
  /deep/ .el-radio-button__inner {
    background: inherit;
    background-color: #fff;
    border: 1px solid #569ce1 !important;
    border-radius: 4px !important;
    box-shadow: none !important;
    color: #569ce1;
    font-size: 14px;
    height: 30px;
    line-height: 10px;
    margin-right: 10px;
    min-width: 90px;
  }
  /deep/ .el-radio-button__orig-radio:checked + .el-radio-button__inner:hover {
    background-color: #4d8cca;
    border: 1px solid #4d8cca !important;
    color: #fff;
  }

  /deep/ .el-radio-button__inner:hover {
    border: 1px solid #4d8cca !important;
    color: #4d8cca;
  }
}
.tab-btn {
  float: right;
  z-index: 1 !important;
  position: relative;
  margin-left: 10px;
}
.el-button {
  min-width: 90px;
}
.add-comment {
  display: flex;
  padding-top: 20px;
  border-top: 1px solid #cacbcf;
  .nav-item-icon {
    width: 35px;
    padding-left: 20px;
    [class^="zeta-icon-"],
    [class*=" zeta-icon-"] {
      font-size: 24px;
      color: #94bfe1;
    }
    .avatar-bg {
      background-position: center;
      background-size: 30px 30px;
      border-radius: 15px;
      display: block;
      height: 30px;
      width: 30px;
    }
    .avatar {
      background-position: center;
      background-size: 30px 40px;
      border-radius: 15px;
      display: block;
      height: 30px;
      width: 30px;
    }
  }
}
.submit-btn {
  float: right;
  margin-top: 45px;
}
.zeta-icon-export {
  cursor: pointer;
  font-size: 20px;
  color: #569ce1;
  margin-right: 5px;
}
.zeta-icon-export:hover {
  color: #4d8cca;
}
$color: #ebb563;
.readonly-popup {
  background-color: #fdf7e9;
  padding: 5px 10px;
  position: absolute;
  top: 0;
  width: calc(100% - 50px);
  span.info {
    &,
    i {
      color: $color;
    }
  }
}
</style>
