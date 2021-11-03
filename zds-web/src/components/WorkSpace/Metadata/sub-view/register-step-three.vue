<template>
  <div class="step-three-content">
    <div class="sample-query-div">
      <div class="title">
        <span>Sample Query</span>
      </div>
      <div class="sample-query-add">
        <MetadataSampleQueryEdit
          ref="edit"
          :data="form"
          :is-new="true"
          :all-title="allTitle"
          :parent="'register'"
          :all-platform="allPlatform"
          @operate="add"
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
                  class="zeta-icon-edit"
                  @click="updateSampleQuery(n)"
                />
              </div>
            </template>
            <template v-else>
              <div class="sample-query-edit">
                <MetadataSampleQueryEdit
                  :data="n"
                  :is-new="false"
                  :all-title="allTitle"
                  :parent="'register'"
                  :all-platform="allPlatform"
                  @operate="add"
                  @cancel="cancel"
                />
              </div>
            </template>
          </el-row>
        </el-card>
        <el-card
          v-for="(n, $i) in getSmartQueryArr"
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
    <!--div class="smart-query-search">
            <SmartQuerySearch :smartQueryTableArr="getSearchTablesArr"/>
        </div-->
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch, Prop } from 'vue-property-decorator';
import MetadataSampleQueryEdit from './metadata-sample-query-edit.vue';
import SmartQuerySearch from './smart-query-search.vue';
import SqlEditor from '@/components/common/Visualization/CodeDisplay.vue';
import Util from '@/services/Util.service';
import conf from './../metadata-config';
import _ from 'lodash';

@Component({
  components: {
    MetadataSampleQueryEdit,
    SmartQuerySearch,
    SqlEditor,
  },
})
export default class RegisterStepThree extends Vue {
  allPlatform: any = this.$store.getters.getAllPlatform || [];
  allTitle: any = [];
  sampleDataArr: any = [];
  cmOptions: any = {
    lineNumbers: false,
  };

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
    this.sampleDataArr = this.$store.getters.getRegisterStepThree || [];
  }
    
  formatSQL (sql: string) {
    return Util.SQLFormatter(sql) || sql;
  }

  getSearchTablesArr () {
    return this.$store.getters.getSearchTablesArr || [];
  }

  getSmartQueryArr () {
    return this.$store.getters.getSmartQueryArr || [];
  }

  platformArr (platforms: string): Array<any> {
    const rs: any = [];
    if (platforms) {
      _.forEach(platforms.split(','), (v: string) => {
        rs.push(v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()).replace('mozart', 'Mozart'));
      });
    }
    return _.intersection(conf.allPlatform, rs);
  }
	
  repalceBr (content: any) {
    return content.replace(new RegExp('\n', 'gm'), '<br/>');
  }
	
  updateSampleQuery (col: any) {
    col.edit = true;
    this.sampleDataArr = _.cloneDeep(this.sampleDataArr);
  }
	
  add (data: any) {
    const index = _.findIndex(this.sampleDataArr, (v: any) => { return v.id == data.id; });
    if (index > -1) {
      this.sampleDataArr[index] = data;
      this.sampleDataArr = _.cloneDeep(this.sampleDataArr);
    } else {
      this.sampleDataArr.push(data);
    }

    this.$store.dispatch('setRegisterStepThree', this.sampleDataArr);
  }

  cancel (data: any) {
    data.edit = false;
    const index = _.findIndex(this.sampleDataArr, (v: any) => { return v.id == data.id; });
    if (index > -1) {
      this.sampleDataArr[index] = data;
      this.sampleDataArr = _.cloneDeep(this.sampleDataArr);
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/metadata.scss';
.step-three-content {
	box-sizing: border-box;
	font-style: normal;
	height: 100%;
	overflow-y: hidden;
	padding: 0 25px;
    width: 100%;
    display: flex;
}
.sample-query-div {
    padding-right: 40px;
    width: 100%;
}
.title {
	color: #1E1E1E;
	display: block;
	font-size: 18px;
	font-weight: 700;
	height: 36px;
	line-height: 36px;
	padding: 20px 0;
}
.sample-query-add, .sample-query-edit {
	width: 60%;
}
.sample-query-content {
	height: calc(100% - 76px - 382px);
	width: 100%;
	overflow-y: auto;
    .el-card {
        border: 0px;
        border-bottom: 1px solid #cacbcf;
        border-radius: 0px;
        font-size: 14px;
        font-style: normal;
        font-weight: 400;
        white-space: pre;
        /deep/ .el-card__body {
            padding-bottom: 10px;
            padding-left: 0;
            padding-right: 0;
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
    }
	.el-card:first-child {
		border-top: 1px solid #cacbcf;
	}
    .zeta-icon-edit {
        color: #569ce1;
        cursor:pointer;
        font-size: 18px;
        margin-left: 10px;
        text-align: center;
    }
    .zeta-icon-edit:hover {
        color: #4d8cca;
    }
}
.sample-query-content::-webkit-scrollbar {
    width: 0;
}
.content-editor {
    margin: 10px 0;
    /deep/ .CodeMirror-gutters {
        border: 0px !important;
    }
}
</style>