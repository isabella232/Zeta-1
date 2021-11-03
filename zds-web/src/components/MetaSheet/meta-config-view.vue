<template>
  <div class="view-container">
    <div class="body">
      <div class="config-header">
          Schema Configuration
      </div>
      <meta-scheme-config
      :mode="mode"
      :data="tableData"
      />
      <div class="config-header" style="margin-bottom:20px;">
          Source Configuration
      </div>
      <meta-source-config :mode="mode" :config="sourceConfig"/>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject, Prop } from "vue-property-decorator";
import { MetaConfigTableRow, SourceConfig, SchemeConfig, MetaSheetConfig, Mode, MODE, MS_STATUS } from '@/types/meta-sheet';
import _ from "lodash";
import MetaSchemeConfig from './schema-config.vue';
import MetaSourceConfig from './source-config.vue';

@Component({
  components: {
    MetaSchemeConfig,
    MetaSourceConfig
  }
})
export default class MetaConfigView extends Vue {
  @Prop() mode: Mode;
  @Prop() config: MetaSheetConfig;

  metaConfig: MetaSheetConfig = _.cloneDeep(this.config);
  metaSheetName: string = this.metaConfig.name;;

  get schemaConfig() {
    return this.metaConfig.schemeConfig;
  }

  get sourceConfig() {
    if(this.metaConfig.metaTableStatus == MS_STATUS.CREATED) {
      this.metaConfig.sourceConfig!.platform = '';
      this.metaConfig.sourceConfig!.metaTableType = '';
      this.metaConfig.sourceConfig!.account = '';
    }
    return this.metaConfig.sourceConfig;
  }

  get tableData(): Array<MetaConfigTableRow> {
    return this.metaConfig.schemeConfig!.tableData;
  }

}
</script>

<style lang="scss" scoped>
$blue: #569ce1;
$bgColor: #f7f7f9;
.view-container {
  padding-bottom: 50px;

  .body {
    max-height: 700px;
    overflow-y: auto;
    .config-header {
      padding: 8px;
      color: $blue;
      text-align: center;
      background-color: $bgColor;
    }
  }

}
</style>
