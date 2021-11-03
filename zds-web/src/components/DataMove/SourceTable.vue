<template>
  <div class="source-container">
    <!-- <div v-if="mode == enumMode.TD">
      <td-table :tdtable="sourceTable.tdTableOption" v-on="$listeners" ref="tdTableRef"/>
    </div> -->
    <div v-if="mode == enumMode.VDM">
      <vdm-table
        ref="vdmTableRef"
        :vdmtable="sourceTable.vdmTableOption"
        v-on="$listeners"
      />
    </div>
    <div v-else-if="enumMode.UPLOAD">
      <upload-table
        ref="uploadTableRef"
        :uploadtable="sourceTable.uploadTableOption"
        v-on="$listeners"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Watch, Prop } from 'vue-property-decorator';
import DMRemoteService from '@/services/remote/DataMove';
import _ from 'lodash';
import UploadButton from './upload-dialog/upload-button.vue';
import UploadDialog from './upload-dialog/upload-dialog.vue';
import VdmTable from './VDMTable.vue';
import UploadTable from './Upload.vue';
import TdTable from './TDTable.vue';
import DataMove from './DataMove.vue';
import TDTable from './TDTable.vue';
import { Mode } from './internal';
@Component({
  components: { UploadButton, UploadDialog, VdmTable, TdTable, UploadTable },
})
export default class SourceTable extends Vue {
  @Inject('dmRemoteService') dmRemoteService: DMRemoteService;
  @Prop() mode: string;

  enumMode = Mode;

  srcPlatformOptions = [
    { key: 'mozart', value: 'numozart'},
    // { key: 'hopper', value: 'hopper' }
  ];
  sourceTable: DataMove.SourceTable = {
    tdTableOption: {
      source: 'numozart',
      fullTableName: '',
      filter: '',
    },
    vdmTableOption: {
      source: 'hermesrno',
      database: '',
      tableName: '',
      viewName: '',
    },
    uploadTableOption: {
      schema: [],
      path: '',
    },
  };

  get tdTableRef () {
    return this.$refs.tdTableRef as TDTable;
  }

  get vdmTableRef () {
    return this.$refs.vdmTableRef as VdmTable;
  }

  get uploadTableRef () {
    return this.$refs.uploadTableRef as UploadTable;
  }

  reset () {
    if (this.mode == Mode.UPLOAD && this.uploadTableRef) {
      this.uploadTableRef.reset();
    }
    if (this.mode == Mode.TD && this.tdTableRef) {
      this.tdTableRef.reset();
    }
    if (this.mode == Mode.VDM && this.vdmTableRef) {
      this.vdmTableRef.reset();
    }
  }

  validateSource (tableInfo: DataMove.SourceTable) {
    const source = tableInfo.tdTableOption && tableInfo.tdTableOption.source ? tableInfo.tdTableOption.source: '';
    if (!source) {
      return;
    }
    const hasSource = (_.map(this.srcPlatformOptions, option => option.value).indexOf(source) >= 0);
    if (!hasSource && tableInfo.tdTableOption) {
      tableInfo.tdTableOption.source = '';
    }
  }
  apply (tableInfo: DataMove.SourceTable) {
    this.validateSource(tableInfo);
    this.$nextTick(() => {
      if (this.mode == Mode.VDM) {
        this.vdmTableRef.apply(tableInfo.vdmTableOption!);
      } else if (this.mode == Mode.TD) {
        this.tdTableRef.apply(tableInfo.tdTableOption!);
      }
    });
  }

  getParams (): DataMove.UploadParam | DataMove.TDTable | DataMove.VDMTable | undefined {
    if (this.mode == Mode.UPLOAD) {
      return this.uploadTableRef.getParams();
    } else if (this.mode == Mode.VDM) {
      return this.sourceTable.vdmTableOption!;
    } else if (this.mode == Mode.TD) {
      return this.sourceTable.tdTableOption!;
    }
  }

  @Watch('mode')
  isSourceValid () {
    this.$emit('is-table-valid', {mode: this.mode, valid: false});
  }

}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.source-container {
  padding: 0 5px;
  /deep/ .row {
        margin-top: 15px;
        margin-bottom: 15px;

        .source-info-label {
            margin-bottom: 10px;
        }
    }
}
</style>


