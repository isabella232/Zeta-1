<template>
  <el-dialog
    v-loading="loading"
    label-width="80px"
    title="Import"
    :visible.sync="visible_"
  >
    <el-radio-group v-model="importType">
      <el-radio :label="0">
        Import file
      </el-radio>
      <el-radio :label="1">
        Import table
      </el-radio>
    </el-radio-group>
    <el-form
      v-if="importType == 1"
      ref="form"
      label-position="left"
      :rules="rule"
      :model="form"
      label-width="120px"
    >
      <el-form-item
        label="Platform:"
        prop="platform"
      >
        <el-select
          v-model="form.platform"
          placeholder="Select platform"
        >
          <el-option
            v-for="item in platformOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        label="DB Name:"
        prop="db"
      >
        <el-input
          v-model="form.db"
          placeholder="Please db name"
        />
      </el-form-item>
      <el-form-item
        label="Table Name:"
        prop="table"
      >
        <el-input
          v-model="form.table"
          placeholder="Please table name"
        />
      </el-form-item>
    </el-form>
    <el-upload
      v-if="importType == 0"
      ref="upload"
      class="upload-demo"
      action
      :limit="1"
      :file-list="fileList"
      accept=".xls, .xlsx, .csv"
      :auto-upload="false"
    >
      <el-button
        slot="trigger"
        size="small"
        type="default"
        plain
      >
        Browse file
      </el-button>
      <a
        class="download-span"
        download="Data_Model_Template.xlsx"
        :href="templateBase64"
      ><i class="zeta-icon-download" />Download template</a>
      <div
        slot="tip"
        class="el-upload__tip"
        style="color: #CACBCF;"
      >
        only support .xls/xlsx/csv file
      </div>
    </el-upload>
    <div
      slot="footer"
      class="dialog-footer"
    >
      <el-button
        type="default"
        plain
        class="cancel-btn"
        @click="cancel()"
      >
        Cancel
      </el-button>
      <el-button
        class="clone-btn"
        type="primary"
        @click="importTable()"
      >
        Import
      </el-button>
    </div>
  </el-dialog>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import conf from './da-config';
import _ from 'lodash';
import { ZetaException } from '@/types/exception';
import XLSX from 'xlsx';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const TemplateBase64 = require('!!url-loader?limit=1!@/assets/Data_Model_Template.xlsx');

@Component({
  components: {},
})
export default class CloneDialog extends Vue {
  @Prop() visible: boolean;
  @Prop() platform: any;
  @Prop() db: any;
  @Prop() table: any;

  @Inject('doeRemoteService')
  doeRemoteService: DoeRemoteService;

  loading = false;
  platformOptions: Array<any> = conf.platform;
  fileList: any = [];
  importType: any = 0;

  form: {
    platform: string;
    db: string;
    table: string;
  };

  rule: {
    platform: Array<any>;
    db: Array<any>;
    table: Array<any>;
  };
  templateBase64 = TemplateBase64;

  set visible_ (e) {
    this.$emit('update:visible', e);
  }

  get visible_ () {
    return this.visible;
  }

  constructor () {
    super();
    this.form = {
      platform: this.platform || '',
      db: this.db || '',
      table: this.table || '',
    };
    this.rule = {
      platform: [
        {
          required: true,
          message: 'please select platform',
          trigger: 'change',
        },
      ],
      db: [
        {
          required: true,
          message: 'please input db name',
          trigger: 'blur',
        },
      ],
      table: [
        {
          required: true,
          message: 'please input table name',
          trigger: 'blur',
        },
      ],
    };
  }

  add () {
    let valid = false;
    (this.$refs['form'] as any).validate((valid_: boolean) => (valid = valid_));
    if (!valid) {
      return;
    }
    this.getColumns();
  }

  cancel () {
    this.$emit('update:visible', false);
  }

  async checkDB () {
    if (!_.isEmpty(this.form.platform) &&
        !_.isEmpty(this.form.db) &&
        !_.isEmpty(this.form.table))  {
      const rs: any = {'platform': this.form.platform,
        'db': this.form.db,
        'table': this.form.table};
      this.loading = true;
      this.doeRemoteService.getModel(rs).then(res => {
        console.debug('Call Api:getModel successed');
        this.loading = false;
        if (res && res.data && res.data.data && res.data.data.value && res.data.data.value[0] && res.data.data.value[0] != {}) {
          this.$emit('clone-data', res.data.data.value[0]);
          this.$emit('update:visible', false);
        } else {
          this.$message.info('No data to clone');
        }
      }).catch((err: ZetaException) => {
        err.message = 'Get clone data error';
        console.debug('Call Api:getModel failed: ' + JSON.stringify(err));
        this.loading = false;
      });
    }
  }

  async getColumns () {
    //if (!this.checkInputTableInfo()) return;
    if (!_.isEmpty(this.form.platform) &&
        !_.isEmpty(this.form.db) &&
        !_.isEmpty(this.form.table))  {
      const params: any = {'platform': this.form.platform,
        'db': this.form.db,
        'table': this.form.table};
      this.loading = true;
      this.doeRemoteService.getColumns(params).then(res => {
        console.debug('Call Api:getColumns successed');
        if (res && res.data && res.data != null) {
          const data: any = _.sortBy(res.data, 'columnid');
          this.$emit('clone-data', data);
          this.$emit('update:visible', false);
          this.loading = false;
        }
      }).catch(err => {
        this.loading = false;
        console.debug('Call Api:getColumns failed: ' + JSON.stringify(err));
      });
    }
  }

  importTable () {
    if (this.importType == 0) {
      this.submitUpload();
    } else {
      this.add();
    }
  }

  submitUpload () {
    /* Boilerplate to set up FileReader */
    const reader = new FileReader();
    reader.onload = (e: any) => {
      const template = conf.excel_template;
      /* Parse data */
      const bstr = e.target.result;
      const wb = XLSX.read(bstr, {type:'binary'});
      /* Get first worksheet */
      const wsname = wb.SheetNames[0];
      const ws = wb.Sheets[wsname];
      /* Convert array of arrays */
      const data = XLSX.utils.sheet_to_json(ws, {header: 1});
      const cols: any = [];
      _.forEach(data[0] as any[], (v: any, i: any) => {
        cols.push({ title: v, index: i });
      });

      const transform = _.transform(_.slice(data, 1), (rs: any, v: any) => {
        const obj: any = {};
        _.forEach(template, (sv: any) => {
          const find: any = _.find(cols, (ssv: any) => { 
            return ssv.title.replace(/\s+/g, '').toLowerCase() === sv.title.replace(/\s+/g, '').toLowerCase();
          });
          if (find) obj[sv.key] = v[find.index];
        });
        rs.push(obj);
      }, []);
      this.$emit('import-data', transform);
      this.$emit('update:visible', false);
    };
    const upload: any = this.$refs.upload;
    reader.readAsBinaryString(upload.uploadFiles[0].raw);
  }

  @Watch('platform')
  onPlatform (val: any) {
    this.form.platform = val;
  }

  @Watch('db')
  onDb (val: any) {
    this.form.db = val;
  }

  @Watch('table')
  onTable (val: any) {
    this.form.table = val;
  }
}
</script>
<style lang="scss" scoped>
.el-dialog {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}
.el-input {
  margin-top: 10px;
}
.el-radio-group {
  margin-bottom: 20px;
}
.download-span {
  color: #CACBCF;
  cursor: pointer;
  text-decoration: none;
  margin-left: 10px;
  .zeta-icon-download {
    color: #CACBCF;
  }
}
.download-span:hover {
  color: #4D8CCA;
  .zeta-icon-download {
    color: #4D8CCA;
  }
}
</style>
