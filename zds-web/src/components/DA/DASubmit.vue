<template>
  <div class="content" v-loading="loading">
    <div @click="closeComment($event)" style="height: 100%">
      <!--el-button @click="back()" type="primary" size="small" icon="el-icon-back">Back</el-button-->
      <el-breadcrumb separator-class="el-icon-arrow-right">
        <el-breadcrumb-item>
          <span class="breadcrumb-clk" @click="back();">DA</span>
        </el-breadcrumb-item>
        <el-breadcrumb-item>details</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="table-content">
        <div class="form">
          <!-- devlopver submit -->
          <el-form
            label-position="left"
            :rules="rule"
            :model="form"
            ref="form"
            label-width="125px"
            v-if="!approveFlag"
          >
            <el-form-item label="Platform:" prop="platform">
              <el-select v-model="form.platform" multiple placeholder="Select platform">
                <el-option-group v-for="group in platformOptions" :key="group.label">
                  <el-option
                    v-for="item in group.options"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                    :disabled="platformType != 'all' && platformType != group.label"
                  ></el-option>
                </el-option-group>
              </el-select>
            </el-form-item>
            <el-form-item label="DB Name:" prop="db">
              <el-input v-model="form.db" placeholder="Please db name"></el-input>
            </el-form-item>
            <el-form-item label="Table Name:" prop="table">
              <div style="display: flex;">
                <el-input v-model="form.table" placeholder="Please table name"></el-input>
              </div>
            </el-form-item>
            <el-form-item label="Subject Area:" prop="sa">
              <el-select
                v-model="form.sa"
                filterable
                placeholder="Select the subject area..."
                :class="saCompare"
                @change="(val) => saChange(val)"
              >
                <el-option
                  v-for="item in subjectAreaOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="Information Layer:">
              <el-select
                v-model="form.info_layer"
                placeholder="Select the information layer..."
                :class="infoLayerCompare"
              >
                <el-option
                  v-for="item in informationLayerOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="Description:">
              <el-input
                type="textarea"
                v-model="form.desc"
                :autosize="{ minRows: 2, maxRows: 4}"
                resize="none"
                placeholder="Please enter description..."
                :class="descCompare"
              ></el-input>
            </el-form-item>
            <el-form-item label="Jira ID:" prop="jira_id">
              <el-input v-model="form.jira_id" placeholder="DWRM-"></el-input>
            </el-form-item>
            <el-form-item label="HDFS Location:" prop="hdfs_lctn" v-if="isHadoopTable">
              <el-input
                v-model="form.hdfs_lctn"
                placeholder="example: /sys/edw/gdw_tables/lstg/dw_lstg_item/snapshot/"
              ></el-input>
            </el-form-item>
            <el-form-item label="Submitter by:" prop="submitter">{{this.form.cre_user}}</el-form-item>
            <el-form-item label="Approver:" prop="approver">{{this.form.approver}}</el-form-item>
          </el-form>
          <!-- da approve -->
          <el-form
            label-position="left"
            :rules="rule"
            :model="form"
            ref="form"
            label-width="150px"
            v-else
          >
            <el-form-item
              label="Platform:"
              prop="platform"
            >{{transformPlatform(this.form.platform)}}</el-form-item>
            <el-form-item label="DB Name:" prop="db">{{this.form.db}}</el-form-item>
            <el-form-item label="Table Name:" prop="table">{{this.form.table}}</el-form-item>
            <el-form-item label="Subject Area:" prop="sa">
              <el-select
                v-model="form.sa"
                filterable
                placeholder="Select the subject area..."
                :disabled="!edit"
                :class="saCompare"
              >
                <el-option
                  v-for="item in subjectAreaOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="Information Layer:">
              <el-select
                v-model="form.info_layer"
                placeholder="Select the information layer..."
                :disabled="!edit"
                :class="infoLayerCompare"
              >
                <el-option
                  v-for="item in informationLayerOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="Description:">{{this.form.desc}}</el-form-item>
            <el-form-item label="Jira ID:">{{this.form.jira_id}}</el-form-item>
            <el-form-item label="HDFS Location:" prop="hdfs_lctn" v-if="isHadoopTable">{{this.form.hdfs_lctn}}</el-form-item>
            <el-form-item label="Submitter by:" prop="submitter">{{this.form.cre_user}}</el-form-item>
            <el-form-item label="Approver:" prop="approver">{{this.form.approver}}</el-form-item>
          </el-form>
        </div>
        <div class="ddl">
          <div class="edit-div" v-if="approveFlag">
            <el-button type="primary" @click="openEdit" v-if="!edit">Edit</el-button>
            <el-button type="default" plain @click="closeEdit" v-if="edit">Cancel</el-button>
            <el-button type="primary" @click="submit" v-if="edit">Save</el-button>
          </div>
          <el-input type="textarea" placeholder="DDL" v-model="ddl"></el-input>
        </div>
      </div>
      <div style="display: flex; flex-direction: row; justify-content: flex-end;">
        <el-button
          class="column-imp-btn"
          type="primary"
          @click="importColumns"
          title="Import column fields by any production or non-production table name"
          v-if="!approveFlag"
        >Import</el-button>
        <el-button
          class="column-add-btn"
          type="primary"
          @click="addRow"
          :disabled="approveFlag && !edit"
        >Add</el-button>
      </div>
      <el-table
        ref="multipleTable"
        :data="tableData"
        border
        :cell-class-name="tableCellClassName"
        class="column-table"
        row-key="id"
        :row-class-name="approveFlag && !edit? 'table-ready-only' : 'table-edit'"
        header-cell-class-name="col-head-css"
        @cell-mouse-enter.once='rowDrop'
      >
        <el-table-column label="Action" width="105" :fixed="fixedColumn">
          <template slot-scope="scope">
            <i
              class="action-icon zeta-icon-edit"
              @click="editRow(scope.row, scope.$index)"
              :disabled="approveFlag && !edit"
            ></i>
            <i
              class="action-icon zeta-icon-delet"
              @click="remove(scope.$index, tableData)"
              :disabled="approveFlag && !edit"
            ></i>
            <i
              class="action-icon el-icon-plus"
              @click="add(scope.$index, tableData)"
              :disabled="approveFlag && !edit"
            ></i>
          </template>
        </el-table-column>
        <el-table-column property="column_name" label="Name" width="180" :fixed="fixedColumn">
          <template slot-scope="scope">
            <span v-if="scope.row.edit">
              <el-input style="width: 100%;" v-model="scope.row.column_name"></el-input>
            </span>
            <span v-else>{{scope.row.column_name}}</span>
          </template>
        </el-table-column>
        <el-table-column property="data_type" label="Data Type" width="180">
          <template slot-scope="scope">
            <span v-if="scope.row.edit">
              <el-input style="width: 100%;" v-model="scope.row.data_type"></el-input>
            </span>
            <span v-else>{{scope.row.data_type}}</span>
          </template>
        </el-table-column>
        <el-table-column property="desc" label="Description" min-width="300">
          <template slot-scope="scope">
            <span v-if="scope.row.edit">
              <el-input
                style="width: 100%;"
                v-model="scope.row.desc"
                type="textarea"
                :autosize="{ minRows: 1}"
                resize="none"
              ></el-input>
            </span>
            <span v-else>{{scope.row.desc}}</span>
          </template>
        </el-table-column>
        <el-table-column property="pk_flag" width="120">
          <template slot="header" slot-scope="scope">
            <span title="Primary Key">PK</span>
          </template>
          <template slot-scope="scope">
            <span v-if="scope.row.edit">
              <el-select v-model="scope.row.pk_flag">
                <el-option key="true" label="TRUE" value="1"></el-option>
                <el-option key="false" label="FALSE" value="0"></el-option>
              </el-select>
            </span>
            <span v-else>{{formatBoolean(scope.row.pk_flag)}}</span>
          </template>
        </el-table-column>
        <el-table-column property="pi_flag" width="120">
          <template slot="header" slot-scope="scope">
            <span title="Primary Index">PI</span>
          </template>
          <template slot-scope="scope">
            <span v-if="scope.row.edit">
              <el-select v-model="scope.row.pi_flag">
                <el-option key="true" label="TRUE" value="1"></el-option>
                <el-option key="false" label="FALSE" value="0"></el-option>
              </el-select>
            </span>
            <span v-else>{{formatBoolean(scope.row.pi_flag)}}</span>
          </template>
        </el-table-column>
        <el-table-column property="mndtry_flag" label="Mandatory" width="120">
          <template slot-scope="scope">
            <span v-if="scope.row.edit">
              <el-select v-model="scope.row.mndtry_flag">
                <el-option key="true" label="TRUE" value="1"></el-option>
                <el-option key="false" label="FALSE" value="0"></el-option>
              </el-select>
            </span>
            <span v-else>{{formatBoolean(scope.row.mndtry_flag)}}</span>
          </template>
        </el-table-column>
        <el-table-column property="pii_flag" width="120">
          <template slot="header" slot-scope="scope">
            <span title="Personally identifiable information">PII</span>
          </template>
          <template slot-scope="scope">
            <span v-if="scope.row.edit">
              <el-select v-model="scope.row.pii_flag">
                <el-option key="true" label="TRUE" value="1"></el-option>
                <el-option key="false" label="FALSE" value="0"></el-option>
              </el-select>
            </span>
            <span v-else>{{formatBoolean(scope.row.pii_flag)}}</span>
          </template>
        </el-table-column>
        <el-table-column property="ppi_flag" width="120">
          <template slot="header" slot-scope="scope">
            <span title="Primary Partition Index">PPI</span>
          </template>
          <template slot-scope="scope">
            <span v-if="scope.row.edit">
              <el-select v-model="scope.row.ppi_flag">
                <el-option key="true" label="TRUE" value="1"></el-option>
                <el-option key="false" label="FALSE" value="0"></el-option>
              </el-select>
            </span>
            <span v-else>{{formatBoolean(scope.row.ppi_flag)}}</span>
          </template>
        </el-table-column>
        <el-table-column property="eic" width="150">
          <template slot="header" slot-scope="scope">
            <span title="eBay Information Classification">EIC</span>
          </template>
          <template slot-scope="scope">
            <span v-if="scope.row.edit">
              <el-select v-model="scope.row.eic">
                <el-option key="Public" label="Public" value="Public"></el-option>
                <el-option key="Internal" label="Internal" value="Internal"></el-option>
                <el-option key="Confidential" label="Confidential" value="Confidential"></el-option>
                <!--el-option key="Restricted" label="Restricted" value="Restricted"></el-option-->
                <el-option key="blank" label="" value=""></el-option>
              </el-select>
            </span>
            <span v-else>{{scope.row.eic}}</span>
          </template>
        </el-table-column>
        <el-table-column property="encryption_flag" label="Encryption" width="120">
          <template slot-scope="scope">
            <span v-if="scope.row.edit">
              <el-select v-model="scope.row.encryption_flag">
                <el-option key="yes" label="Yes" value="1"></el-option>
                <el-option key="no" label="No" value="0"></el-option>
              </el-select>
            </span>
            <span v-else>{{formatYesNo(scope.row.encryption_flag)}}</span>
          </template>
        </el-table-column>
        <el-table-column label="Regulation" width="120">
          <template slot-scope="scope">
            <span>
              <div style="display: flex;">
                <span class="checkbox-lable" @click="scope.row.gdpr = !scope.row.gdpr; scope.row.ccpa = true">GDPR</span>
                <input type="checkbox" v-model="scope.row.gdpr" :disabled="!scope.row.edit" class="checkbox-input" @change="scope.row.ccpa = true"/>
              </div>
              <div style="display: flex;">
                <span class="checkbox-lable" @click="scope.row.ccpa = !scope.row.ccpa">CCPA</span>
                <input type="checkbox" v-model="scope.row.ccpa" :disabled="!(scope.row.edit && !scope.row.gdpr)" class="checkbox-input"/>
              </div>
            </span>
          </template>
        </el-table-column>
      </el-table>
      <!-- devlopver submit -->
      <el-button class="submit-btn" type="primary" @click="submit" v-if="!approveFlag">Submit</el-button>
      <el-button class="save-btn" type="default" plain @click="save(false)" v-if="!approveFlag">Save</el-button>
      <!-- da approve -->
      <el-button
        class="decline-btn"
        type="default"
        plain
        @click="decline"
        v-if="approveFlag"
      >Decline</el-button>
      <el-button class="approve-btn" type="primary" @click="approve" v-if="approveFlag">Approve</el-button>
      <!-- da comment -->
      <el-badge :value="commentsVal" :max="9999" class="comment-icon-div" :type="badgeType">
        <el-button name="comment-btn" icon="zeta-icon-comment-1" @click="openComment" circle></el-button>
      </el-badge>
    </div>
    <MetadataComment
      ref="metadataComment"
      :data="commentsData"
      vueRef="DA"
      @submit-comment="submitComent"
      :loading="commentLoading"
    />
    <clone-dialog
      :visible.sync="cloneDialogVisible"
      :platform="this.form.platform"
      :db="this.form.db"
      :table="this.form.table"
      @clone-data="cloneData"
      @import-data="importData"
    />
    <el-dialog :visible.sync="dialogVisible" :close-on-click-modal="false" width="333px">
      <span slot="title" class="dialog-header">
        <i class="zeta-icon-info"></i>
        <span>DDL</span>
      </span>
      <span v-html="message" />
      <span slot="footer" class="dialog-footer">
        <el-button type="default" plain @click="closeDialog">Back & Update</el-button>
        <el-button type="default" plain @click="addModel">Save</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script lang="ts">
import uuid from 'uuid';
import { Component, Vue, Prop, Watch, Inject } from "vue-property-decorator";
import MetadataComment from "@/components/WorkSpace/Metadata/sub-view/metadata-comment.vue";
import CloneDialog from "@/components/DA/CloneDialog.vue";
import _ from "lodash";
import DoeRemoteService from "@/services/remote/DoeRemoteService";
import moment from "moment";
import Util from "@/services/Util.service";
import conf from "./da-config";
import sortable from "sortablejs";
import { ZetaException, ExceptionPacket } from "@/types/exception";

function parseBoolField(val: any) {
  switch (typeof val) {
    case "number":
      return val > 0;
    case "boolean":
      return val;
    case "string":
      val = val.trim().toLowerCase();
      return val === "true" || val === "y";
    default:
      return false;
  }
}

function mapBoolField(val: any) {
  return parseBoolField(val) ? "1" : "0";
}

@Component({
  components: {
    MetadataComment,
    CloneDialog
  }
})
export default class DASubmit extends Vue {
  @Prop() data: any;
  @Prop({ default: false })
  approveFlag: boolean;
  @Prop() cloneFlg: boolean;

  @Inject("doeRemoteService") doeRemoteService: DoeRemoteService;

  loading: boolean = false;
  tableData: Array<any> = [];
  fixedColumn: boolean = false;
  commentsData: any = {};
  commentLoading: boolean = false;
  isExistTable: boolean = false;
  submitDisabled: boolean = true;
  lastSubmitData: Array<any> = [];
  lastSubmit: any = {};
  saSubmitData: Array<any> = [];
  edit: boolean = false;
  temp_data: any = {};
  last_edit_data: any = {};
  commentsVal: number = 0;
  cloneDialogVisible: boolean = false;
  ddl: string = "";
  status: string = "";
  dialogVisible: boolean = false;
  message: string = "";
  toAddr: string = "";
  oldPath: string = "";
  saOwnerNt: string = "";

  platformOptions: Array<any> = conf.platformGroup;
  subjectAreaOptions: Array<any> = [];
  informationLayerOptions: Array<any> = [];

  form: {
    platform: any;
    db: string;
    table: string;
    sa: string;
    desc: string;
    table_tier: any;
    flbck_flag: any;
    load_method: string;
    load_source: string;
    info_layer: string;
    status: string;
    jira_id: string;
    cre_user: string;
    approverNt: string;
    approver: string;
    hdfs_lctn: string;
  };

  rule: {
    platform: Array<any>;
    db: Array<any>;
    table: Array<any>;
    jira_id: Array<any>;
    sa: Array<any>;
    hdfs_lctn: Array<any>;
  };

  constructor() {
    super();
    this.last_edit_data = _.cloneDeep(this.data);
    this.form = {
      platform: this.cloneFlg
        ? []
        : this.data.pltfrm_name
        ? this.data.pltfrm_name.split(",")
        : [],
      db: this.data.db_name || "",
      table: this.data.table_name || "",
      sa: this.data.sbjct_area || "",
      desc: this.data.table_desc || "",
      table_tier: this.data.tier_cd || null,
      flbck_flag: this.data.flbck_flag || null,
      load_method: this.data.load_method || "",
      load_source: this.data.load_source || "",
      info_layer: this.data.info_layer || "",
      status: this.cloneFlg ? "new" : this.data.status || "new",
      jira_id: this.data.jira_id || "",
      cre_user: this.data.cre_user || Util.getNt(),
      approverNt: "",
      approver: "",
      hdfs_lctn: ""
    };

    if (!_.isEmpty(this.data.sbjct_area)) this.saChange(this.data.sbjct_area);

    this.rule = {
      platform: [
        {
          required: !this.approveFlag,
          message: "Must have platform",
          trigger: "change"
        }
      ],
      db: [
        {
          required: !this.approveFlag,
          message: "Must have db name",
          trigger: "blur"
        }
      ],
      table: [
        {
          required: !this.approveFlag,
          message: "Must have table name",
          trigger: "blur"
        },
        {
          validator: (r: any, f: string, cb: Function) => {
            const reg1 = new RegExp("^((?!._merge$).)*$");
            const reg2 = new RegExp("^((?!._sort$).)*$");
            if (
              reg1.exec(this.form.table) === null ||
              typeof reg1.exec(this.form.table) === "undefined" ||
              reg2.exec(this.form.table) === null ||
              typeof reg2.exec(this.form.table) === "undefined"
            ) {
              cb(new Error("Can not use '_merge' or '_sort' tables."));
            } else cb();
          }
        }
      ],
      sa: [
        {
          required: true,
          message: "Must have the subject area",
          trigger: "change"
        }
      ],
      jira_id: [
        {
          required: !this.approveFlag,
          message: "Must have jira_id",
          trigger: "blur"
        },
        {
          validator: (r: any, f: string, cb: Function) => {
            const reg = new RegExp("[0-9]+");
            const i = this.form.jira_id.toUpperCase().indexOf("DWRM-");
            if (i < 0) {
              cb(new Error('The jira_id must include "DWRM-".'));
            } else if (
              reg.exec(this.form.jira_id.substring(i)) === null ||
              typeof reg.exec(this.form.jira_id.substring(i)) === "undefined"
            ) {
              cb(new Error("The release label must include jira number."));
            } else cb();
          }
        }
      ],
      hdfs_lctn: [
        {
          required: !this.approveFlag && this.isHadoopTable,
          message: "Must have HDFS location",
          trigger: "blur"
        }
      ]
    };
  }

  get platformType() {
    if (!_.isEmpty(this.form.platform)) {
      return _.indexOf(
        conf.hadoop_platform,
        _.upperFirst(this.form.platform[0])
      ) > -1
        ? "spark"
        : "td";
    }

    return "all";
  }

  get isHadoopTable(): boolean {
    return !_.isEmpty(this.form.platform)
      ? _.indexOf(conf.hadoop_platform, _.upperFirst(this.form.platform[0])) >
          -1
      : false;
  }

  get saCompare() {
    if (
      (this.status == "new" || this.status == "modified") &&
      !_.isEmpty(this.lastSubmit)
    ) {
      if (this.form.sa != this.lastSubmit.sbjct_area) {
        return "warning";
      }
    }
    return "";
  }

  get infoLayerCompare() {
    if (
      (this.status == "new" || this.status == "modified") &&
      !_.isEmpty(this.lastSubmit)
    ) {
      if (this.form.info_layer != this.lastSubmit.info_layer) {
        return "warning";
      }
    }
    return "";
  }

  get descCompare() {
    if (
      (this.status == "new" || this.status == "modified") &&
      !_.isEmpty(this.lastSubmit)
    ) {
      if (this.form.desc != this.lastSubmit.table_desc) {
        return "warning";
      }
    }
    return "";
  }

  get badgeType() {
    return this.commentsVal <= 0 ? "info" : "danger";
  }

  mounted() {
    if (!_.isUndefined(this.data)) {
      this.checkDB();
    }

    this.getInformationLayerOptions();

    this.doeRemoteService
      .getSA()
      .then(res => {
        console.debug("Call Api:getSA successed");
        if (res && res.data && res.data != null) {
          if (
            !_.isUndefined(res.data.data) &&
            res.data.data.hasOwnProperty("value")
          ) {
            _.forEach(res.data.data.value, (v: any) => {
              if (!_.isUndefined(v.subject_area)) {
                const option: any = {
                  label: v.subject_area,
                  value: v.subject_area
                };
                this.subjectAreaOptions.push(option);
              }
            });
          }
        }

        if (
          !_.isUndefined(this.subjectAreaOptions) &&
          !_.isEmpty(this.subjectAreaOptions)
        ) {
          this.subjectAreaOptions = _.sortBy(this.subjectAreaOptions, [
            "value"
          ]);
        }
      })
      .catch(err => {
        err.resolve();
        console.debug("Call Api:getSA failed: " + JSON.stringify(err));
      });
  }

  rowDrop() {
    const tbody: any = document.querySelector(
      ".column-table .el-table__body-wrapper tbody"
    );
    const _this = this;
    sortable.create(tbody, {
      filter: ".table-ready-only",
      setData: function(dataTransfer) {
        dataTransfer.setData("Text", "");
      },
      // onChoose() {
      //   _this.fixedColumn = false
      // },
      // onUnchoose: function (evt) {
      //   _this.fixedColumn = true
      // },
      onEnd({ newIndex, oldIndex }: any) {
        let tempList = _.cloneDeep(_this.tableData);
        const currRow = tempList.splice(oldIndex, 1)[0];
        tempList.splice(newIndex, 0, currRow);
        _this.tableData = tempList;
        _this.createDDL();
      }
    });
  }

  tableCellClassName({ row, column, rowIndex }: any) {
    if (this.status == "new" && !_.isEmpty(this.lastSubmit)) {
      const index = _.findIndex(this.lastSubmitData, (v: any) => {
        return _.toLower(v.column_name) == _.toLower(row.column_name);
      });
      if (index == -1) {
        return "warning-row";
      } else if (
        _.toLower(row[column.property]) !==
        _.toLower(this.lastSubmitData[index][column.property])
      ) {
        return "warning-row";
      }
    }
    return "";
  }

  back() {
    this.$emit("back");
  }

  addRow() {
    const defRow = {
      id: uuid(),
      column_name: "",
      data_type: "",
      desc: null,
      eic: null,
      encryption_flag: "0",
      gdpr: false,
      ccpa: false,
      pk_flag: "0",
      pi_flag: "0",
      mndtry_flag: "0",
      pii_flag: "0",
      ppi_flag: "0",
      edit: true
    };
    this.tableData.push(defRow);
  }

  remove(index: number, rows: Array<any>) {
    rows.splice(index, 1);
  }

  add(index: number, rows: Array<any>) {
    const defRow = {
      column_name: "",
      data_type: "",
      desc: null,
      eic: null,
      encryption_flag: "0",
      gdpr: false,
      ccpa: false,
      pk_flag: "0",
      pi_flag: "0",
      mndtry_flag: "0",
      pii_flag: "0",
      ppi_flag: "0",
      edit: true
    };

    rows.splice(index + 1, 0, defRow);
  }

  editRow(row: any, index: number) {
    row["edit"] = true;
  }

  submit() {
    let valid: boolean = false;
    (this.$refs["form"] as any).validate((valid_: boolean) => (valid = valid_));
    if (!valid) {
      console.debug("Invalid submit");
      return;
    }

    if (!this.approveFlag) {
      const find: any = _.find(this.tableData, (v: any) => {
        return _.trim(v.desc) == "";
      });
      if (find) {
        const message = "Column " + find.column_name + " must have descprtion";
        let ex = new ZetaException(
          <ExceptionPacket>{
            code: "",
            errorDetail: {
              message: message
            }
          },
          {
            path: "da"
          }
        );
        this.$store.dispatch("addException", { exception: ex });
        return;
      }
    }
    this.createDDL();
    this.save(true);
  }

  save(isSubmitFlg: boolean) {
    if (!isSubmitFlg) {
      if (!this.checkInputTableInfo()) return;
      if (this.form.status.indexOf("-draft") == -1)
        this.form.status =
          this.form.status == "approved"
            ? "modified-draft"
            : this.form.status + "-draft";
    } else if (isSubmitFlg && this.form.status.indexOf("-draft") > -1) {
      this.form.status = this.form.status.replace("-draft", "");
    }

    if (!this.approveFlag) {
      this.form.cre_user = Util.getNt();
      if (!isSubmitFlg) this.createDDL();
      if (this.form.status == "approved" && isSubmitFlg)
        this.form.status = "modified";
    }
    this.message = "";
    if (this.approveFlag && !this.checkDDL()) {
      this.dialogVisible = true;
      return;
    }
    this.addModel(isSubmitFlg);
  }

  checkInputTableInfo() {
    if (_.isEmpty(this.form.platform)) {
      let ex = new ZetaException(
        <ExceptionPacket>{
          code: "",
          errorDetail: {
            message: "Must have platform"
          }
        },
        {
          path: "da"
        }
      );
      this.$store.dispatch("addException", { exception: ex });
      return false;
    }

    if (_.isEmpty(this.form.db)) {
      let ex = new ZetaException(
        <ExceptionPacket>{
          code: "",
          errorDetail: {
            message: "Must have db name"
          }
        },
        {
          path: "da"
        }
      );
      this.$store.dispatch("addException", { exception: ex });
      return false;
    }

    if (_.isEmpty(this.form.table)) {
      let ex = new ZetaException(
        <ExceptionPacket>{
          code: "",
          errorDetail: {
            message: "Must have table name"
          }
        },
        {
          path: "da"
        }
      );
      this.$store.dispatch("addException", { exception: ex });
      return false;
    }

    return true;
  }

  addModel(isSubmitFlg: boolean) {
    this.dialogVisible = false;
    let rs: any = _.cloneDeep(this.form);
    let columns: any = this.tableData;
    const ddl: any = this.ddl;
    rs = { ...rs, ddl, columns };
    rs.platform = _.join(this.form.platform);
    const reg = new RegExp("[0-9]+");
    rs.jira_id =
      "DWRM-" +
      reg.exec(
        this.form.jira_id.substring(
          this.form.jira_id.toUpperCase().indexOf("DWRM-")
        )
      );
    if (
      !this.cloneFlg &&
      !_.isEmpty(this.last_edit_data.db_name) &&
      !_.isEmpty(this.last_edit_data.table_name) &&
      (this.last_edit_data.table_name != rs.table || this.last_edit_data.db_name != rs.db)
    ) {
      rs.old_table = {
        db: this.last_edit_data.db_name,
        table: this.last_edit_data.table_name
      };
    }
    this.loading = true;
    this.doeRemoteService
      .addModelByMultiPltfrm(rs)
      .then(res => {
        console.debug("Call Api:addModel successed");
        this.last_edit_data.db_name = rs.db;
        this.last_edit_data.table_name = rs.table;
        if (res && res.status == 200) {
          if (!isSubmitFlg) {
            this.$message.success("Save success");
          } else if (!this.approveFlag) {
            const link: any =
              `${location.protocol}//${location.host}/${Util.getPath()}#/da?` +
              `platform=` +
              _.join(this.form.platform, ",") +
              `&&db=` +
              this.form.db +
              `&&table=` +
              this.form.table;
            const table: any =
              _.join(this.form.platform, ",") +
              "." +
              this.form.db +
              "." +
              this.form.table;
            const content: any = {
              name: "All",
              msg:
                "Data model of table " +
                table +
                " need review, please check it.<br/>" +
                "Please <a href=" +
                link +
                ">click</a>"
            };
            this.sendMail(content, "Submit Success");
          } else {
            this.edit = false;
            this.$message.success("Submit success");
          }
        }
        this.loading = false;
      })
      .catch(err => {
        console.debug("Call Api:getModel failed: " + JSON.stringify(err));
        this.loading = false;
      });
  }

  checkDB() {
    const platform = this.cloneFlg
      ? this.data.pltfrm_name.split(",")
      : this.form.platform;
    const db = this.cloneFlg ? this.data.db_name : this.form.db;
    const table = this.cloneFlg ? this.data.table_name : this.form.table;
    if (!_.isEmpty(platform) && !_.isEmpty(db) && !_.isEmpty(table)) {
      this.commentsVal = 0;
      const rs: any = { platform: platform, db: db, table: table };
      this.loading = true;
      this.edit = false;
      this.doeRemoteService
        .getModelByMultiPltfrm(rs)
        .then(res => {
          console.debug("Call Api:getModel successed");
          if (res && res.data && res.data != null) {
            if (
              !_.isEmpty(res.data) &&
              !_.isUndefined(res.data.data) &&
              !_.isUndefined(res.data.data.value) &&
              !_.isEmpty(res.data.data.value)
            ) {
              const value: any =
                !_.isUndefined(res.data.data.value) &&
                res.data.data.value.length > 0
                  ? res.data.data.value[0]
                  : res.data.data.value;
              this.commentsData = value;
              this.commentsVal = value.comments
                ? value.comments.length || 0
                : 0;
              this.status = this.cloneFlg ? "new" : value.status;
              if (this.status == "new" || this.status == "modified")
                this.getPreModel();
              this.ddl = value.ddl;
              this.form.desc =
                !_.isUndefined(value.table_desc) && !_.isNull(value.table_desc)
                  ? value.table_desc
                  : "";
              this.form.sa = !_.isUndefined(value.sbjct_area)
                ? value.sbjct_area
                : "";
              this.form.table_tier = !_.isUndefined(value.tier_cd)
                ? value.tier_cd
                : null;
              this.form.flbck_flag = !_.isUndefined(value.flbck_flag)
                ? value.flbck_flag
                : null;
              this.form.load_method = !_.isUndefined(value.load_method)
                ? value.load_method
                : "";
              this.form.load_source = !_.isUndefined(value.load_source)
                ? value.load_source
                : "";
              this.form.info_layer = !_.isUndefined(value.info_layer)
                ? value.info_layer
                : "";
              this.form.jira_id = !_.isUndefined(value.jira_id)
                ? value.jira_id
                : "";
              this.form.cre_user = !_.isUndefined(value.cre_user)
                ? value.cre_user
                : "";
              this.form.status = this.cloneFlg ? "new" : value.status;
              this.form.approverNt = value.status != "approved" ? "" : "";
              this.form.approver = this.cloneFlg
                ? ""
                : !_.isUndefined(value.approver)
                ? value.approver
                : "";
              this.form.hdfs_lctn = !_.isUndefined(value.hdfs_lctn)
                ? value.hdfs_lctn
                : "";
              this.oldPath = value.hdfs_lctn;
              this.tableData = [];
              let rs: any = [];
              _.forEach(_.cloneDeep(value.columns), (v: any, i: any) => {
                //v["isSet"] = true;
                v["id"] = uuid();
                v["desc"] = v["column_desc"];
                v["encryption_flag"] = _.isUndefined(v["encryption_flag"])
                  ? "0"
                  : v["encryption_flag"].toString();
                v["gdpr"] = _.isUndefined(v["gdpr"])
                  ? false
                  : (v["gdpr"] == 1 ? true : false);
                v["ccpa"] = _.isUndefined(v["ccpa"])
                  ? false
                  : (v["ccpa"] == 1 ? true : false)
                v["pk_flag"] = _.isUndefined(v["pk_flag"])
                  ? "0"
                  : v["pk_flag"].toString();
                v["pi_flag"] = _.isUndefined(v["pi_flag"])
                  ? "0"
                  : v["pi_flag"].toString();
                v["mndtry_flag"] = _.isUndefined(v["mndtry_flag"])
                  ? "0"
                  : v["mndtry_flag"].toString();
                v["pii_flag"] = _.isUndefined(v["pii_flag"])
                  ? "0"
                  : v["pii_flag"].toString();
                v["ppi_flag"] = _.isUndefined(v["ppi_flag"])
                  ? "0"
                  : v["ppi_flag"].toString();
                v["edit"] = false;
                rs.push(v);
              });
              this.tableData = rs;
            } else {
              this.ddl = "";
              this.form.desc = "";
              this.form.sa = "";
              this.form.table_tier = null;
              this.form.flbck_flag = null;
              this.form.load_method = "";
              this.form.load_source = "";
              this.form.info_layer = "";
              this.form.status = "new";
              this.form.jira_id = "";
              this.form.cre_user = Util.getNt();
              this.form.approverNt = "";
              this.form.hdfs_lctn = "";
              this.tableData = [];
              const value: any = {
                pltfrm_name: this.form.platform,
                db_name: this.form.db,
                table_name: this.form.table,
                comments: []
              };
              if (!this.approveFlag) this.edit = true;
              this.commentsData = value;
              this.lastSubmitData = [];
              this.lastSubmit = {};
              this.oldPath = "";
            }
          }
          this.loading = false;
        })
        .catch(err => {
          console.debug("Call Api:getModel failed: " + JSON.stringify(err));
          this.loading = false;
          this.lastSubmitData = [];
          this.lastSubmit = {};
          this.oldPath = "";
        });
    }
  }

  async getPreModel() {
    if (
      !_.isEmpty(this.form.platform) &&
      !_.isEmpty(this.form.db) &&
      !_.isEmpty(this.form.table)
    ) {
      const rs: any = {
        platform: this.form.platform[0],
        db: this.form.db,
        table: this.form.table
      };
      this.doeRemoteService
        .getPrevModelByMultiPltfrm(rs)
        .then(res => {
          console.debug("Call Api:getPrevModel successed");
          this.lastSubmitData = [];
          this.lastSubmit = {};
          if (res && res.data && res.data != null) {
            if (
              !_.isEmpty(res.data) &&
              !_.isUndefined(res.data.data) &&
              !_.isUndefined(res.data.data.value) &&
              !_.isEmpty(res.data.data.value)
            ) {
              const value: any =
                !_.isUndefined(res.data.data.value) &&
                res.data.data.value.length > 0
                  ? res.data.data.value[0]
                  : res.data.data.value;
              this.lastSubmit = value;
              let rs: any = [];
              _.forEach(_.cloneDeep(value.columns), (v: any) => {
                v["desc"] = v["column_desc"];
                v["encryption_flag"] = _.isUndefined(v["encryption_flag"])
                  ? "0"
                  : v["encryption_flag"].toString();
                v["gdpr"] = _.isUndefined(v["gdpr"])
                  ? false
                  : (v["gdpr"] == 1 ? true : false);
                v["ccpa"] = _.isUndefined(v["ccpa"])
                  ? false
                  : (v["ccpa"] == 1 ? true : false)
                v["pk_flag"] = _.isUndefined(v["pk_flag"])
                  ? "0"
                  : v["pk_flag"].toString();
                v["pi_flag"] = _.isUndefined(v["pi_flag"])
                  ? "0"
                  : v["pi_flag"].toString();
                v["mndtry_flag"] = _.isUndefined(v["mndtry_flag"])
                  ? "0"
                  : v["mndtry_flag"].toString();
                v["pii_flag"] = _.isUndefined(v["pii_flag"])
                  ? "0"
                  : v["pii_flag"].toString();
                v["ppi_flag"] = _.isUndefined(v["ppi_flag"])
                  ? "0"
                  : v["ppi_flag"].toString();
                rs.push(v);
              });
              this.lastSubmitData = rs;
            }
          }
        })
        .catch(err => {
          err.resolve();
          console.debug("Call Api:getPrevModel failed: " + JSON.stringify(err));
          this.lastSubmitData = [];
          this.lastSubmit = {};
        });
    }
  }

  formatBoolean(flag: any): string {
    if (parseInt(flag) == 1) {
      return "TRUE";
    } else {
      return "FALSE";
    }
  }

  formatYesNo(flag: any): string {
    if (parseInt(flag) == 1) {
      return "Yes";
    } else {
      return "No";
    }
  }

  importColumns() {
    this.cloneDialogVisible = true;
  }

  cloneData(value: any) {
    this.tableData = [];
    let rs: any = [];
    _.forEach(_.cloneDeep(value), (v: any) => {
      v["id"] = uuid();
      v["column_name"] = v["columnname"];
      v["data_type"] = v["columntype"];
      v["desc"] = v["desc"];
      v["eic"] = v["eic"];
      v["encryption_flag"] = mapBoolField(v["encryption_flag"]);
      v["gdpr"] = mapBoolField(v["gdpr"]);
      v["ccpa"] = mapBoolField(v["ccpa"]);
      v["pk_flag"] = mapBoolField(v["pk_flag"]);
      v["pi_flag"] =
        v["pi_flag"] === "UPI" || v["pi_flag"] === "NUPI" ? "1" : "0";
      v["mndtry_flag"] = mapBoolField(v["mndtry_flag"]);
      v["pii_flag"] = mapBoolField(v["pii_flag"]);
      v["ppi_flag"] = mapBoolField(v["ppi_flag"]);
      v["edit"] = false;
      rs.push(v);
    });
    this.tableData = rs;
  }

  importData(value: any) {
    if (_.isEmpty(this.tableData)) {
      this.cloneData(value);
    } else {
      let tmp = [...this.tableData];
      this.cloneData(value);
      this.tableData = this.tableData.concat(tmp);
    }
  }

  decline() {
    const link: any =
      `${location.protocol}//${location.host}/${Util.getPath()}#/da?` +
      `platform=` +
      _.join(this.form.platform, ",") +
      `&&db=` +
      this.form.db +
      `&&table=` +
      this.form.table;
    const table: any =
      _.join(this.form.platform, ",") +
      "." +
      this.form.db +
      "." +
      this.form.table;
    const content: any = {
      name: "All",
      msg:
        "Data model of table " +
        table +
        " is declined by DA.<br/>" +
        "Please <a href=" +
        link +
        ">click</a>"
    };
    this.sendMail(content, "Decline success");
  }

  approve() {
    const params: any = {
      platform: _.join(this.form.platform, ","),
      db: this.form.db,
      table: this.form.table,
      approver: Util.getNt()
    };

    this.loading = true;
    this.doeRemoteService
      .approveModelByMultiPltfrm(params)
      .then(res => {
        console.debug("Call Api:approveModel success");
        if (res && res.status == 200) {
          const link: any =
            `${location.protocol}//${location.host}/${Util.getPath()}#/da?` +
            `platform=` +
            _.join(this.form.platform, ",") +
            `&&db=` +
            this.form.db +
            `&&table=` +
            this.form.table;
          const table: any =
            _.join(this.form.platform, ",") +
            "." +
            this.form.db +
            "." +
            this.form.table;
          const content: any = {
            name: "All",
            msg:
              "Data model of table " +
              table +
              " is approved by DA.<br/>" +
              "Please <a href=" +
              link +
              ">click</a>"
          };
          this.sendMail(content, "Approve Success");
        }
        this.loading = false;
      })
      .catch(err => {
        console.debug("Call Api:getModel failed: " + JSON.stringify(err));
        this.loading = false;
      });
  }

  async sendMail(content: any, message: any, nts?: any) {
    const params: any = {
      fromAddr: conf.email_from_addr,
      toAddr: conf.email_to_da_addr,
      subject: conf.email_default_subject,
      content: JSON.stringify(content),
      template: "ZetaNotification",
      ccAddr: nts ? "" : conf.email_cc_addr,
      type: 3 //1: html; 2: txt
    };
    if (!nts) nts = [this.form.cre_user];
    nts = _.uniq(nts);
    for await (const nt of nts) {
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
          params.toAddr = params.toAddr + v.mail + ";";
        });
      } else {
        // TODO LOG
        // window.$logger.error(["EMAIL ADDR ERROR", "Get empty email address by " + nt, res.config.url], "Get empty email address by " + nt);
      }
    }
    if (process.env.VUE_APP_ENV == "production") {
      params.toAddr = params.toAddr + (this.saOwnerNt + "@ebay.com");
    } else {
      console.log(
        "DEV/QA TEST:" + params.toAddr + (this.saOwnerNt + "@ebay.com")
      );
    }
    if (!_.isEmpty(params.toAddr)) {
      this.doeRemoteService
        .createEmail(params)
        .then(res => {
          console.debug("Call Api:createEmail successed");
          this.loading = false;
          if (res && res.status == 200) {
            if (message) this.$message.success(message);
          }
        })
        .catch(err => {
          this.loading = false;
          console.debug("Call Api:createEmail failed: " + JSON.stringify(err));
          this.$message.info(message + ", but send mail failed");
        });
    } else {
      this.loading = false;
      this.$message.info(message + ", but empty recipient mailbox");
    }
  }

  openComment() {
    const child: any = this.$refs.metadataComment;
    child.open();
  }

  closeComment(e: any) {
    if (
      e.target.name !== "comment-btn" &&
      e.target.className !== "zeta-icon-comment-1"
    ) {
      const child: any = this.$refs.metadataComment;
      child.close();
    }
  }

  submitComent(params: any) {
    this.commentLoading = true;
    params.platform = _.join(this.form.platform, ",");
    this.doeRemoteService
      .addModelCommentByMultiPltfrm(params)
      .then(res => {
        console.debug("Call Api:addModelCommentByMultiPltfrm successed");
        if (res && res.status == 200) {
          const rs: any = {
            platform: this.form.platform,
            db: this.form.db,
            table: this.form.table
          };
          this.doeRemoteService
            .getModelByMultiPltfrm(rs)
            .then(res => {
              console.debug("Call Api:getModel successed");
              if (res && res.data && res.data != null) {
                if (
                  !_.isEmpty(res.data) &&
                  !_.isUndefined(res.data.data) &&
                  !_.isUndefined(res.data.data.value)
                ) {
                  this.commentLoading = false;
                  const value: any =
                    !_.isUndefined(res.data.data.value) &&
                    res.data.data.value.length > 0
                      ? res.data.data.value[0]
                      : res.data.data.value;
                  this.commentsData = value;
                  this.commentsVal = value.comments
                    ? value.comments.length || 0
                    : 0;
                  const child: any = this.$refs.metadataComment;
                  child.cancel();
                  const link: any =
                    `${location.protocol}//${
                      location.host
                    }/${Util.getPath()}#/da?` +
                    `platform=` +
                    _.join(this.form.platform, ",") +
                    `&&db=` +
                    this.form.db +
                    `&&table=` +
                    this.form.table;
                  const content: any = {
                    name: "All",
                    msg:
                      "@" +
                      params.cre_user +
                      " add a new comment.<br/>" +
                      params.comment +
                      "<br/>" +
                      "Please <a href=" +
                      link +
                      ">click</a>"
                  };
                  this.sendMail(content, "Submit Success", [
                    params.cre_user,
                    this.form.cre_user
                  ]);
                } else {
                  this.commentLoading = false;
                  this.$message.info("Comment get error! Please refresh page.");
                }
              }
            })
            .catch(err => {
              this.commentLoading = false;
            });
        }
        this.commentLoading = false;
      })
      .catch(err => {
        console.debug(
          "Call Api:addModelCommentByMultiPltfrm failed: " + JSON.stringify(err)
        );
        this.commentLoading = false;
      });
  }

  openEdit() {
    this.edit = true;
    this.temp_data.form = _.cloneDeep(this.form);
    this.temp_data.tableData = _.cloneDeep(this.tableData);
  }

  closeEdit() {
    this.edit = false;
    if (this.temp_data.form) this.form = _.cloneDeep(this.temp_data.form);
    if (this.temp_data.tableData)
      this.tableData = _.cloneDeep(this.temp_data.tableData);
  }

  isTDPlatform(platform) {
    platform = _.toLower(platform);
    return (
      platform === "numozart" || platform === "mozart" || platform === "hopper"
    );
  }

  createDDL() {
    const startIndex: number = _.indexOf(this.ddl, "(");
    const endIndex: number = this.getDDLColumnsEndIndex();
    const isTDPlatform = this.isTDPlatform(this.form.platform);
    if (startIndex < endIndex) {
      const columns: any = this.fetchColumnsFromDDL(startIndex, endIndex);
      let newColumns: any = [];
      _.forEach(_.cloneDeep(this.tableData), (v: any) => {
        const find: any = _.find(columns, (sv: any) => {
          return (
            _.toLower(_.trim(sv).split(" ")[0]) == _.toLower(v.column_name)
          );
        });
        if (find) {
          newColumns.push(find);
        } else {
          newColumns.push("    '" + v.column_name + "' " + v.data_type);
        }
      });
      const content = _.join(newColumns, ",\r\n");
      let temp: string = "";
      if (this.cloneFlg) {
        let header: string = isTDPlatform
          ? conf.td_multiset_header
          : conf.spark_sql_header;
        let footer: string = isTDPlatform
          ? conf.td_multiset_footer
          : conf.spark_sql_footer;
        temp =
          header.replace("$table", this.form.db + "." + this.form.table) +
          " (\n" +
          content +
          "\n)" +
          footer.replace("$path", this.form.hdfs_lctn);
      } else {
        temp =
          this.ddl.substring(0, startIndex) +
          "(\n" +
          content +
          "\n)" +
          this.ddl.substring(endIndex + 1);
        if (!isTDPlatform && !_.isEmpty(this.oldPath)) {
          temp = temp.replace(this.oldPath, this.form.hdfs_lctn);
        }

        if (
          this.last_edit_data.db_name != this.form.db &&
          temp.indexOf(this.last_edit_data.db_name + ".") > -1
        )
          temp = temp.replace(this.last_edit_data.db_name + ".", this.form.db + ".");
        if (
          this.last_edit_data.table_name != this.form.table &&
          temp.indexOf("." + this.last_edit_data.table_name + " ") > -1
        )
          temp = temp.replace(
            "." + this.last_edit_data.table_name + " ",
            "." + this.form.table + " "
          );
      }

      this.ddl = temp;
    } else {
      if (_.isEmpty(this.ddl)) {
        let temp: string = "";
        let header: string = isTDPlatform
          ? conf.td_multiset_header
          : conf.spark_sql_header;
        let footer: string = isTDPlatform
          ? conf.td_multiset_footer
          : conf.spark_sql_footer;

        temp =
          header.replace("$table", this.form.db + "." + this.form.table) +
          " (\n";
        _.forEach(this.tableData, (v: any, k: any) => {
          temp +=
            "    '" +
            v.column_name +
            "' " +
            v.data_type +
            (k == this.tableData.length - 1 ? "" : ",") +
            "\n";
        });
        temp = temp + ")" + footer.replace("$path", this.form.hdfs_lctn);
        this.ddl = temp;
      }
    }
  }

  checkDDL() {
    const startIndex: number = _.indexOf(this.ddl, "(");
    const endIndex: number = this.getDDLColumnsEndIndex();
    let missCol: string = "";
    let notExistCol: string = "";
    let positionErrCol: string = "";
    if (startIndex < endIndex) {
      const columns: any = this.fetchColumnsFromDDL(startIndex, endIndex);
      for (let i = 1; i < this.tableData.length; i++) {
        const findIndex = _.findIndex(columns, (v: any) => {
          return (
            _.toLower(v.split(" ")[0]).indexOf(
              _.toLower(this.tableData[i].column_name)
            ) > -1
          );
        });
        // findIndex == -1：ddl missing this column; findIndex != i: col postion error
        if (findIndex == -1) {
          missCol =
            missCol +
            (missCol.length > 0
              ? ", " + _.toLower(this.tableData[i].column_name)
              : _.toLower(this.tableData[i].column_name));
        } else if (findIndex != i) {
          positionErrCol =
            positionErrCol +
            (positionErrCol.length > 0
              ? ", " + _.toLower(this.tableData[i].column_name)
              : _.toLower(this.tableData[i].column_name));
        }
      }

      _.forEach(columns, (v: any) => {
        const findIndex = _.findIndex(this.tableData, (sv: any) => {
          return (
            _.toLower(v.split(" ")[0]).indexOf(_.toLower(sv.column_name)) > -1
          );
        });
        // table missing this column
        if (findIndex == -1) {
          notExistCol =
            notExistCol +
            (notExistCol.length > 0
              ? ", " + _.toLower(v.split(" ")[0]).replace(/["|'|`]/g, "")
              : _.toLower(v.split(" ")[0]).replace(/["|'|`]/g, ""));
        }
      });
    } else {
      this.message = "ddl recognition error";
      return false;
    }

    if (
      !_.isEmpty(missCol) ||
      !_.isEmpty(notExistCol) ||
      !_.isEmpty(positionErrCol)
    ) {
      let message: string = "";
      if (!_.isEmpty(missCol)) {
        message =
          message +
          (message.length > 0 ? ",<br/>" : "") +
          "ddl missing the following columns: " +
          missCol;
      }

      if (!_.isEmpty(notExistCol)) {
        message =
          message +
          (message.length > 0 ? ",<br/>" : "") +
          "table missing the following columns: " +
          notExistCol;
      }

      if (!_.isEmpty(positionErrCol)) {
        message =
          message +
          (message.length > 0 ? ",<br/>" : "") +
          "the following locations is incorrect: " +
          positionErrCol;
      }

      this.message = message;
      return false;
    }

    return true;
  }

  getDDLColumnsEndIndex() {
    if (!_.isEmpty(this.ddl)) {
      const bracketsArr: any = this.ddl.match(/\(|\)/g) || [];
      if (bracketsArr && bracketsArr.length > 0) {
        let index: number = 0;
        let left: number = 0;
        let right: number = 0;
        for (let i = 0; i < bracketsArr.length; i++) {
          if (bracketsArr[i] == "(") {
            left++;
          } else if (bracketsArr[i] == ")") {
            right++;
            index = _.indexOf(this.ddl, ")", index + 1);
          }

          if (left == right) return index;
        }
      }
    }

    return -1;
  }

  fetchColumnsFromDDL(startIndex: number, endIndex: number) {
    const temp = this.ddl.substring(startIndex + 1, endIndex);
    const removeBr: string = temp.replace(/[\r\n]/g, "") || "";
    let columns: any = removeBr.split(/,\s*(?=[^)^\]^>]*(?:\(|<|\[|$))/g) || [];
    return columns;
  }

  closeDialog() {
    this.dialogVisible = false;
  }

  transformPlatform(platformList: any) {
    return _.join(
      _.transform(
        platformList,
        (rs: any, v: any) => {
          rs.push(_.upperFirst(_.toLower(v)));
        },
        []
      ),
      ","
    );
  }

  saChange(val: any) {
    this.saOwnerNt = "";
    this.doeRemoteService
      .getModelSaOwner(val)
      .then(res => {
        if (res && res.data && res.data.data && res.data.data.value) {
          if (_.size(res.data.data.value) > 0) {
            this.saOwnerNt = res.data.data.value[0].prmry_da_nt
              ? res.data.data.value[0].prmry_da_nt
              : "";
          }
        }
      })
      .catch(err => {
        err.resolve();
        console.debug(
          "Call Api:getModelSaOwner failed: " + JSON.stringify(err)
        );
      });
  }

  getInformationLayerOptions() {
    this.doeRemoteService.getInformationLayerOptions().then(res => {
      this.informationLayerOptions = res;
    })
  }

  @Watch("form.platform")
  onPlatformChange() {
    this.rule.hdfs_lctn.pop();
    this.rule.hdfs_lctn.push({
      required: !this.approveFlag && this.isHadoopTable,
      message: "Must have HDFS location",
      trigger: "blur"
    });
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
$bc-height: $workspace-tab-height + $workspace-tab-margin-bottom;
.content {
  width: 100%;
  height: calc(100% - #{$bc-height});
  overflow-y: auto;
}
.table-content {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  justify-content: flex-start;
}
.title {
  font-weight: 700;
  font-style: normal;
  font-size: 24px;
  margin: 22px 0;
}
.el-form {
  /deep/ .el-textarea {
    width: 507px;
  }
  /deep/ .el-input {
    width: 507px;
  }
}
.el-button {
  min-width: 90px;
  height: 30px;
  font-size: 14px;
}
.submit-btn {
  margin-top: 22px;
  float: right;
  margin-right: 65px;
}
.save-btn {
  margin-top: 22px;
  float: right;
  margin-right: 5px;
}
.clone-btn {
  margin: auto 0;
  margin-left: 22px;
}
.back-btn {
  margin-top: 22px;
}
.el-table {
  margin-top: 22px;
  /deep/ .el-button {
    background: inherit;
    background-color: rgba(255, 255, 255, 0);
    border-width: 1px;
    border-style: solid;
    border-color: rgba(86, 156, 225, 1);
    border-radius: 4px;
    box-shadow: none;
    color: #569ce1;
  }
  /deep/ .warning-row {
    color: #e53917;
  }
  /deep/ .checkbox-lable {
    width: 45px;
    line-height: 20px;
    cursor: pointer;
  }
  /deep/ .checkbox-input {
    margin: 3px 0px;
    cursor: pointer;
  }
  .action-icon {
    color: #569ce1;
    cursor: pointer;
    font-size: 18px;
    padding: 0 3px;
  }
  /deep/.col-head-css {
    color: #000 !important;
  }
}
.approve-btn {
  float: right;
  margin-top: 22px;
  margin-right: 5px;
}
.decline-btn {
  float: right;
  margin-top: 22px;
  background: inherit;
  background-color: rgba(255, 255, 255, 0);
  border-width: 1px;
  border-style: solid;
  border-color: rgba(86, 156, 225, 1);
  border-radius: 4px;
  box-shadow: none;
  color: #569ce1;
  margin-right: 65px;
}
.comment-icon-div {
  bottom: 20px;
  position: fixed;
  right: 20px;
  width: 73px;
  z-index: 3;
  /deep/ .el-badge__content {
    top: 10px;
    right: 35px;
  }
  .el-button {
    height: auto;
    border-color: #fff;
    box-shadow: 0 0 10px #cacbcf;
    color: #569ce1;
    min-width: 0;
  }
  .el-button:hover {
    background-color: #fff;
    color: #4d8cca;
  }
  /deep/ [class^="zeta-icon-"],
  [class*=" zeta-icon-"] {
    font-size: 25px !important;
  }
}
.ddl {
  width: 100%;
  /deep/ .el-textarea {
    box-sizing: border-box;
    padding-left: 20px;
    padding-top: 5px;
  }

  /deep/ .el-textarea__inner {
    max-height: 420px !important;
    min-height: 420px !important;
    height: 420px !important;
    overflow-y: auto;
  }
}
.edit-div {
  float: right;
}
.dialog-header {
  [class^="zeta-icon-"],
  [class*=" zeta-icon-"] {
    font-size: 20px;
  }
  .zeta-icon-info {
    color: #f3af2b;
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
.warning {
  /deep/ .el-input__inner {
    color: #e53917;
  }
  /deep/ .el-textarea__inner {
    color: #e53917;
  }
}
.el-breadcrumb {
  margin-bottom: 10px;
  .breadcrumb-clk {
    cursor: pointer;
    color: #4d8cca;
  }
  .breadcrumb-clk:hover {
    color: #569ce1;
    text-decoration: underline;
  }
}
</style>