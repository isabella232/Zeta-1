<template>
  <div class="my-collection-tabdiv">
    <div class="header">
      <el-select
        v-model="selTableArr"
        multiple
        collapse-tags
        filterable
        remote
        reserve-keyword
        placeholder="Search tables"
        :remote-method="fetchSuggestions"
        :loading="loadingOpt"
      >
        <el-option
          v-for="item in options"
          :key="item.value"
          :label="item.label"
          :value="item.value"
          :disabled="getStatus(item)"
        >
          <span>{{ item.label }}</span>
          <span v-if="getStatus(item)" class="request-row">
            <el-tooltip
              content="Bottom center"
              placement="bottom"
              effect="light"
              popper-class="dependency-info-popup"
            >
              <div slot="content">
                no done file for this table in metadata.
                <br />Click to
                <span class="request" @click.stop="goRequest(item)">request</span> table owner provide dependency signal.
              </div>
              <i class="el-icon-question" style="color: #606266" />
            </el-tooltip>
          </span>
        </el-option>
      </el-select>
      <el-button type="primary" @click="onAdd" :disabled="(selTableArr.length) <= 0">Add</el-button>
      <el-button type="danger" @click="onDelete" :disabled="(multipleSelection.length) <= 0">Delete</el-button>
    </div>

    <el-table
      ref="multipleTable"
      :data="collectionData"
      style="margin-top:32px;"
      @selection-change="handleSelectionChange"
      height="calc(100% - 86px)"
    >
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column property="collection_name" label="My Table List" />
      <el-table-column property="pltfrm_name" label="platform">
        <template slot-scope="scope">
          <i
            v-for="p in scope.row.pltfrm_name.split(',')"
            :key="p"
            :title="p"
            :class="p.toLowerCase()"
            class="platform-icon"
          />
        </template>
      </el-table-column>
      <!--el-table-column property="update" label="Update Time" />
      <el-table-column property="average finish time" label="Average Finish Time" /-->
    </el-table>
  </div>
</template>

<script lang="ts">
/* eslint-disable @typescript-eslint/camelcase */
import { Vue, Component, Prop, Watch } from "vue-property-decorator";
import DoeRemoteService from "@/services/remote/DoeRemoteService";
import _ from "lodash";
import Util from "@/services/Util.service";
import { ZetaException } from "@/types/exception";

interface TableItem {
  value: string;
  label: string;
  table: string;
  db: string;
  info: {
    type: string;
    [key: string]: string;
  };
}

@Component({
  components: {}
})
export default class MyCollection extends Vue {
  @Prop() collectionData: any;

  doeRemoteService: DoeRemoteService = DoeRemoteService.instance;
  selTableArr: any = [];
  options: any = [];
  loading: boolean = false;
  loadingOpt: boolean = false;
  multipleSelection: any = [];

  mounted() {
    this.fetchSuggestions("");
  }

  fetchSuggestions(searchStr: string) {
    this.loadingOpt = true;
    this.options = [];
    this.doeRemoteService
      .queryTable(searchStr, 50)
      .then(res => {
        if (res) {
          // this.options = _.uniq(
          //   _.map(res, (v: any) => {
          //     return _.toUpper(v.db + "." + v.table);
          //   })
          // );
          this.options = _.uniqBy(
            _.transform(
              res,
              (rs: any, v: any) => {
                rs.push({
                  value: _.toUpper(v.db + "." + v.table),
                  label: _.toUpper(v.db + "." + v.table),
                  table: _.toUpper(v.table),
                  db: _.toUpper(v.db),
                  info: v.info
                });
              },
              []
            ),
            "value"
          );
        }
        _.remove(this.options, (v: any) => {
          return _.find(this.collectionData, (sv: any) => {
            return sv.collection_name == v.label;
          });
        });
        this.loadingOpt = false;
      })
      .catch((err: any) => {
        this.loadingOpt = false;
        console.error("get Table list err: " + JSON.stringify(err));
      });
  }

  onAdd() {
    this.loading = true;
    this.doeRemoteService
      .insertMyCollectionList({
        nt: Util.getNt(),
        collectionList: this.selTableArr
      })
      .then(res => {
        if (res && res.status == 200) {
          this.$emit("query-collection");
          this.selTableArr = [];
        }
      })
      .catch((err: any) => {
        this.loading = false;
        console.error("insert Table list err: " + JSON.stringify(err));
      });
  }

  onDelete() {
    this.loading = true;
    this.doeRemoteService
      .deleteMyCollectionList({
        nt: Util.getNt(),
        collectionList: _.map(this.multipleSelection, "collection_name")
      })
      .then(res => {
        if (res && res.status == 200) {
          this.$emit("query-collection");
          this.multipleSelection = [];
        }
      })
      .catch((err: any) => {
        this.loading = false;
        console.error("delete Table list err: " + JSON.stringify(err));
      });
  }

  handleSelectionChange(val) {
    this.multipleSelection = val;
  }

  getStatus(item: TableItem) {
    return item.info.type === "unknown" ? true : false;
  }

  goRequest(item: TableItem) {
    const content = {
      name: "All",
      msg: `[${Util.getNt()}] requests for dependency signal of table [${
        item.value
      }]
        <br/>
        <br/>
        Table Owner - ${item.info.prdct_owner_nt}
        <br/>
        <br/>
        Guide – to enable dependency signal for table, please contact Bin Song with done file provided.
      `
    };
    const param = {
      subject: `Request Dependency Signal of table [${item.value}]`,
      content: content,
      ccAddr: "",
      sae: item.info.prmry_dev_sae_nt
    };
    this.sendMail(param);
  }

  async sendMail(param: any) {
    const toAddr = ["stachen@ebay.com", "gzhu@ebay.com", "binsong@ebay.com"];
    toAddr.push(`${Util.getNt()}@ebay.com`);
    if (param.sae) {
      toAddr.push(`${param.sae}@ebay.com`);
    }

    const params: any = {
      fromAddr: "DL-eBay-ZETA@ebay.com",
      toAddr: toAddr.join(";"), // toAddr.join(';')
      subject: param.subject,
      content: JSON.stringify(param.content),
      template: "ZetaNotification",
      ccAddr: "",
      type: 3 //1: html; 2: txt
    };
    if (!_.isEmpty(params.toAddr)) {
      this.doeRemoteService
        .createEmail(params)
        .then(res => {
          console.error("Call Api:createEmail successed");
          if (res && res.status == 200) {
            this.$message.success("Request success");
          }
        })
        .catch((err: ZetaException) => {
          err.resolve();
          console.error("Call Api:createEmail failed: " + JSON.stringify(err));
          this.$message.info("Request failed");
        });
    } else {
      this.$message.info("empty recipient mailbox");
    }
  }

  @Watch("collectionData")
  onCollectionDataChange(val: any) {
    _.remove(this.selTableArr, (v: any) => {
      return _.find(this.collectionData, (sv: any) => {
        return sv.collection_name == v.label;
      });
    });
    _.remove(this.options, (v: any) => {
      return _.find(this.collectionData, (sv: any) => {
        return sv.collection_name == v.label;
      });
    });
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/metadata.scss";

.my-collection-tabdiv {
  color: red;
  height: 100%;
}
.header {
  /deep/ .el-select {
    width: 400px;
    height: 30px;
    margin-right: 20px;
  }
  /deep/ .el-input__inner {
    height: 30px !important;
  }
}
.request-row {
  margin-left: 5px;
  cursor: pointer !important;
}
.request {
  cursor: pointer !important;
  text-decoration: underline;
  color: #569ce1;
}
.request:hover {
  color: #4d8cca;
}
</style>
