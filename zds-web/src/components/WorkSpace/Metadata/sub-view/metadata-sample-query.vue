<template>
  <div class="sample-query">
    <div class="sample-query-div">
      <div class="sample-query-filter">
        <el-checkbox-group
          v-model="filterPlatform"
          size="small"
          @change="filter"
        >
          <el-checkbox-button
            label="All Platform"
            border
          >
            All Platform
          </el-checkbox-button>
          <el-checkbox-button
            v-for="item in allPlatform"
            :key="item"
            :label="item.toLowerCase()"
            border
          >
            {{ item.replace("NuMozart", "Mozart") }}
          </el-checkbox-button>
        </el-checkbox-group>
      </div>
      <div class="sample-query-input">
        <el-input
          v-if="!edit"
          class="write-query-input"
          placeholder="Write query"
          @focus="openEdit"
        />
        <MetadataSampleQueryEdit
          v-else
          :data="form"
          :is-new="true"
          :all-title="allTitle"
          :all-platform="allPlatform"
          @operate="update"
        />
      </div>
      <div class="sample-query-content">
        <el-card
          v-for="(n, $i) in sampleDataArr"
          :id="$i"
          :key="$i"
          shadow="never"
        >
          <el-row>
            <template v-if="!n.edit">
              <div class="card-head">
                <p class="card-title">
                  {{ n.title }}
                </p>
              </div>
              <div class="card-subhead">
                <div
                  v-for="obj in platformArr(n.platforms)"
                  :key="obj"
                >
                  <i
                    :class="obj.toLowerCase()"
                    class="platform-icon"
                  />
                </div>
              </div>
              <SqlEditor
                class="content-editor"
                :value="formatSQL(n.query_text)"
                :options="cmOptions"
                :read-only="false"
              />
              <div class="card-end">
                <div class="nav-item-icon">
                  <span class="avatar-bg">
                    <dss-avatar
                      inline
                      size="small"
                      cycle
                      :nt="n.upd_user"
                      class="avatar"
                    />
                  </span>
                  <span style="margin-left: 5px;">{{ n.upd_user }} {{ n.upd_date }}</span>
                </div>
                <i
                  v-if="isOwner(n.upd_user)"
                  class="zeta-icon-delet"
                  @click="del(n)"
                />
                <i
                  v-if="isOwner(n.upd_user)"
                  class="zeta-icon-edit"
                  @click="updateSampleQuery(n)"
                />
              </div>
            </template>
            <template v-else>
              <MetadataSampleQueryEdit
                :data="n"
                :is-new="false"
                :all-platform="allPlatform"
                @operate="update"
              />
            </template>
          </el-row>
        </el-card>
        <el-card
          v-for="(n, $i) in smartQueryArrAfterFilter"
          :id="'smartquery' + $i"
          :key="'smartquery' + $i"
          shadow="never"
        >
          <el-row>
            <template v-if="!n.edit">
              <div class="card-head">
                <p class="card-title">
                  Smart Query {{ $i + 1 }}
                </p>
                <div class="smart-icon">
                  Engine by Smart Query
                </div>
              </div>
              <div class="card-subhead">
                <div
                  v-for="obj in platformArr(n.platform)"
                  :key="obj"
                >
                  <i
                    :class="obj.toLowerCase()"
                    class="platform-icon"
                  />
                </div>
                <el-tooltip
                  class="item"
                  effect="light"
                  placement="bottom"
                >
                  <div slot="content">
                    <p style="font-weight: 700px;">
                      How to run on Hadoop?
                    </p><br>Copy the smaple query in SparkSql notebook.<br>Click convert in notebook to change the sample<br>query into SparkSQL
                  </div>
                  <i class="el-icon-question" />
                </el-tooltip>
              </div>
              <SqlEditor
                class="content-editor"
                :value="formatSQL(n.query_text)"
                :options="cmOptions"
                :read-only="false"
              />
              <div class="card-end">
                <div class="nav-item-icon">
                  <i class="zeta-icon-zeta_logo_BETA-1" />
                  <span style="margin-left: 5px;">zeta</span>
                </div>
              </div>
            </template>
            <template v-else>
              <MetadataSampleQueryEdit
                :data="n"
                :is-new="false"
                :all-platform="allPlatform"
                @operate="update"
              />
            </template>
          </el-row>
        </el-card>
      </div>
    </div>
    <div class="smart-query-search">
      <SmartQuerySearch :smart-query-table-arr="smartQueryTableArr" />
    </div>
    <el-dialog
      :visible.sync="dialogVisible"
      :close-on-click-modal="false"
      :show-close="false"
      width="333px"
    >
      <span>Are you sure to delete?</span>
      <span
        slot="footer"
        class="dialog-footer"
      >
        <el-button
          type="primary"
          @click="delSamplquery"
        >Delete</el-button>
        <el-button
          type="default"
          plain
          @click="back"
        >Cancel</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import MetadataSampleQueryEdit from './metadata-sample-query-edit.vue';
import SmartQuerySearch from './smart-query-search.vue';
import SqlEditor from '@/components/common/Visualization/CodeDisplay.vue';
import Util from '@/services/Util.service';
import _ from 'lodash';
import conf from './../metadata-config';
@Component({
  components: {
	  MetadataSampleQueryEdit,
    SmartQuerySearch,
    SqlEditor,
  },
})
export default class MetadataSampleQuery extends Vue {
  @Prop() data: any;
  @Prop() metadataTableDict: any;
  @Prop() metadataViewDict: any;
  @Prop() smartQueryArr: any;
  @Prop() smartQueryTableArr: any;
  cmOptions: any = {
    lineNumbers: false,
  };
  allPlatform: any = [];
  filterPlatform: Array<any> = ['All Platform'];
  edit = false;
  isIndeterminate = true;
  sampleDataArr: any = [];
  smartQueryArrAfterFilter: any = [];
  allTitle: any = [];
  dialogVisible = false;
  delSampleQuery: any;

  form: {
    title: string;
    platform: any;
    query_text: string;
  };

  constructor () {
    super();
    this.form = {
      platform: [],
      title: '',
      query_text: '',
    };

    const rs: any = [];
    if (this.metadataTableDict && !_.isEmpty(this.metadataTableDict)) {
      const arr = _.uniq(_.map(this.metadataTableDict, 'platform'));
      _.forEach(arr, (v: any) => {
        rs.push(v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()).replace('mozart', 'Mozart'));
      });
    }
        
    if (this.metadataViewDict && !_.isEmpty(this.metadataViewDict)) {
      const arr = _.uniq(_.map(this.metadataViewDict, 'platform'));
      _.forEach(arr, (v: any) => {
        rs.push(v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()).replace('mozart', 'Mozart'));
      });
    }
    this.allPlatform = _.intersection(conf.allSampleQueryPlatform, _.uniq(rs));
  }

  mounted () {
    this.getData();
  }

  getData () {
    if (!_.isEmpty(this.filterPlatform)) {
      if (this.filterPlatform[0] == 'All Platform' && this.filterPlatform.length == 1) {
        this.sampleDataArr = this.data;
        this.smartQueryArrAfterFilter = this.smartQueryArr;
      } else {
        this.sampleDataArr = _.filter(this.data, (v: any) => {
          let matchFlg = false;
          if (v.platforms) {
            _.forEach(v.platforms.split(','), (sv: string) => {
              matchFlg = _.indexOf(this.filterPlatform, _.toLower(_.toLower(sv) == 'mozart' ? 'numozart' : sv)) > -1 || matchFlg;
            });
          }
          return matchFlg;
        });
        this.smartQueryArrAfterFilter = _.filter(this.smartQueryArr, (v: any) => {
          let matchFlg = false;
          if (v.platform) {
            _.forEach(v.platform.split(','), (sv: string) => {
              matchFlg = _.indexOf(this.filterPlatform, _.toLower(_.toLower(sv) == 'mozart' ? 'numozart' : sv)) > -1 || matchFlg;
            });
          }
          return matchFlg;
        });
      }
    }
  }

  formatSQL (str: string) {
    const sql = str ? _.join(_.filter(str.split('\n'), (v: any) => { return !_.isEmpty(v.trim()); }), '\n') || str : '';
    return Util.SQLFormatter(sql) || sql;
  }

  editStatus (col: any): boolean {
    return col.edit;
  }

  platformArr (platforms: string): Array<any> {
    let rs: any = [];
    if (platforms) {
      _.forEach(platforms.split(','), (v: string) => {
        rs.push(_.toLower(v));
      });
    }
    rs = _.intersection(conf.allPlatformLowercaseWithMozart, rs);
    if (_.find(rs, (v: any) => { return v == 'mozart'; })) {
      _.remove(rs, (n: any) => { return n == 'nnumozart'; });
    }
    return rs;
  }

  filter () {
    if (!_.isEmpty(this.filterPlatform)) {
      if (_.last(this.filterPlatform) == 'All Platform') {
        this.filterPlatform = ['All Platform'];
      } else {
        const filterArr: any = _.cloneDeep(this.filterPlatform);
        _.remove(filterArr, (v: string) => {return v == 'All Platform';});
        this.filterPlatform = filterArr;
      }
    } else {
      this.filterPlatform = ['All Platform'];
    }
    this.getData();
  }

  clearEdit () {
    this.form = {
      platform: [],
      title: '',
      query_text: '',
    };
  }

  openEdit () {
    this.edit = true;
    this.form.platform = this.filterPlatform;
    this.allTitle = _.map(this.sampleDataArr, 'title');
  }

  updateSampleQuery (col: any) {
    col.edit = true;
    this.sampleDataArr = _.cloneDeep(this.sampleDataArr);
  }

  cancel () {
    this.edit = false;
    this.clearEdit();
  }

  update (params: any, isNew: boolean, isSave: boolean) {
    if (isSave) {
      this.$emit('submit-sample-query', params, isNew);
    } else {
      if (params) {
        const find: any = _.find(this.sampleDataArr, (v: any) => {return v.sample_query_id == params.sample_query_id;});
        find.edit = false; 
      } else {
        this.edit = false;
      }
            
    }
  }

  repalceBr (content: any) {
    return content.replace(new RegExp('\n', 'gm'), '<br/>');
  }

  isOwner (userNt: any): boolean {
    const nt: any = Util.getNt();
    return _.isEqual(nt, userNt);
  }

  del (params: any) {
    this.delSampleQuery = params;
    this.dialogVisible = true;
  }

  delSamplquery () {
    this.$emit('delete-sample-query', this.delSampleQuery);
  }

  back () {
    this.delSampleQuery = undefined;
    this.dialogVisible = false;
  }

  @Watch('data')
  onDataChange (val: any) {
    this.getData();
  }

  @Watch('smartQueryArr')
  onSmartQueryArr () {
    this.getData();
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/metadata.scss';
$max_width: calc(100% - 420px);
.sample-query{
    display: flex;
	height: 100%;
    width: 100%;
}
.sample-query-div {
    padding-right: 40px;
    width: $max_width;
}
.smart-query-search {
    border-left: 1px solid #cacbcf;
    width: 420px;
    height: 100%;
}
.sample-query-filter {
    margin-top: 10px;
    .el-checkbox-button {
        /deep/ .el-checkbox-button__inner { 
            background: inherit;
            background-color: #fff;
            border: 1px solid #569ce1 !important;
            border-radius: 4px !important;
            box-shadow: none;
            color: #569ce1;
            font-size: 14px;
            height: 30px;
            line-height: 10px;
            margin-right: 10px;
            min-width: 90px;
        }
       /deep/ .el-checkbox-button__inner:hover {
            border: 1px solid #4d8cca !important;
            color: #4d8cca;
        }
    }
    .el-checkbox-button.is-checked /deep/.el-checkbox-button__inner {
        background-color: #569ce1;
        color: #fff;
    }
    .el-checkbox-button.is-checked /deep/.el-checkbox-button__inner:hover {
        background-color:#4d8cca !important;
        border: 1px solid #4d8cca !important;
        color: #fff;
    }
}

.sample-query-content {
	height: calc(100% - 120px);//sample-query-filter(height:30px;margin-top:10px;),sample-query-input(height:60px;padding:10px,0;)
	width: 100%;
	overflow-y: auto;
    .el-card {
        border: 0px;
        border-bottom: 1px solid #cacbcf;
        border-radius: 4px;
        font-size: 14px;
        font-style: normal;
        font-weight: 400;
        white-space: unset;
        /deep/ .el-card__body {
            padding-bottom: 10px;
            padding-left: 0;
        }
        .card-head {
            display: flex;
            justify-content: left;
        }
        .card-subhead {
            display: flex;
            justify-content: left;
            padding-top: 10px;
        }
        .card-title {
            font-size: 16px;
            font-weight: 700;
            margin-right: 5px;
        }
        .card-content {
            padding: 10px 0px;
            height: auto;
            word-wrap:break-word;
            word-break:break-all;
            overflow: hidden;
            white-space: normal;
        }
        .card-end {
            color: #999999;
            display: flex;
            line-height: 30px;
            padding-left: 5px;
        }
        .nav-item-icon {
            display: inline-flex;
            align-items: center;
            [class^="zeta-icon-"],
            [class*=" zeta-icon-"] {
                font-size: 24px;
                color: #94bfe1;
            }
            .avatar-bg {
                border-radius: 15px;
                display: block;
                background-position: center;
                width: 30px;
                height: 30px;
                background-size: 30px 30px;
            }
            .avatar {
                border-radius: 15px;
                display: block;
                background-position: center;
                width: 30px;
                height: 30px;
                background-size: 30px 40px;
            }
        }
        .smart-icon {
            background: inherit;
            background-color: rgba(92, 184, 92, 1);
            border-radius: 4px;
            box-shadow: none;
            font-size: 10px;
            height: 15px;
            line-height: 15px;
            margin-right: 10px;
            padding: 0 5px;
            text-align: center;
            color: #FFF;
            margin-top: 5px;
        }
    }
    [class^="zeta-icon-"], [class*=" zeta-icon-"] {
        color: #569ce1;
        cursor:pointer;
        font-size: 18px;
        margin-left: 10px;
        text-align: center;
    }
    [class^="zeta-icon-"], [class*=" zeta-icon-"]:hover {
        color: #4d8cca;
    }
}
.sample-query-content::-webkit-scrollbar {
    width: 0;
}
.sample-query-input {
    padding: 10px 0px;
    .write-query-input {
        margin: 10px 0px;
        height: 40px;
        line-height: 40px;
    }
}
.el-icon-question {
    padding-left: 5px;
    padding-top: 3px;
    color: #cacbcf;
}
.content-editor {
    margin: 10px 0;
    /deep/ .CodeMirror-gutters {
        border: 0px !important;
    }
}
.dialog-footer {
	display: flex;
	flex-direction: row-reverse;
	justify-content: center;
    .el-button {
        margin: 0 5px;
    }
}
</style>
