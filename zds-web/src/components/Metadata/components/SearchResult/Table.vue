<template>
  <div>
    <span
      class="content-name"
      @click="jump"
    >{{ data.name }}</span>
    <span
      v-if="infoLayer === 'idl'"
      class="idl-icon"
    >IDL</span>
    <i class="table type-icon" />
    <div style="display: flex; margin: 5px 0;">
      <span style="color: #ff9900; line-height: 20px; margin-right: 5px;">
        {{ scoreStr }}
      </span>
      <el-rate
        :value="score / 2"
        disabled
      />
    </div>
    <div
      v-if="data.desc"
      class="content-desc"
    >
      {{ data.desc }}
    </div>
    <div>
      <i
        v-for="(p, $j) in platforms"
        :key="$j"
        :title="p"
        :class="p.toLowerCase()"
        class="platform-icon"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import { attempt } from '@drewxiu/utils';
import * as Logger from '@/plugins/logs/zds-logger';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
import Util from '@/services/Util.service';

@Component({})
export default class Table extends Vue {

  @Inject() apmTransaction: any;
  @Prop() data: any;
  @Prop() pos: any;

  score = 0;

  get scoreStr () {
    return this.score ? this.score.toFixed(1) : '0.0';
  }
  get platforms () {
    return attempt(() =>
      this.data.parsedContent._source.platform.split(',')
    , []);
  }
  get infoLayer () {
    return this.data.parsedContent._source.info_layer;
  }
  mounted () {
    this.getScore();
  }
  async getScore () {
    const { name, parsedContent } = this.data;
    const type = parsedContent._source.row_type || '';
    this.score = await DoeRemoteService.instance
      .getConfidenceScore(type == 'vdm' ? name.split('.')[1] : name)
      .then((res: any) => {
        if (res && res.data && res.data.data && res.data.data.value) {
          if (res.data.data.value.length > 0) {
            return res.data.data.value[0].score;
          }
        }
      });
  }
  jump () {
    let { name, parsedContent } =  this.data;
    name = name.toUpperCase();
    this.$store.dispatch('setBrowseSearchRoute', {url: window.location.href});
    Logger.counter('METADATA_TABLE_DETAIL', 1, {
      name: 'overview',
      trigger: 'metadata_browse_search_click',
      table: name,
    });
    const type: any = parsedContent._source.row_type || 'table';
    sessionStorage.setItem('metadata_table', name.toUpperCase());
    sessionStorage.setItem('metadata_table_type', type);
    const transaction = this.apmTransaction('metadata', 'view', this.pos);    
    this.$router.push('/metadata/tables/' + name, () => transaction && transaction.end());
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/metadata.scss';

.idl-icon {
  display: inline-block;
  border-radius: 861112px;
  border: 1px solid #cacbcf;
  color: #cacbcf;
  margin-left: 5px;
  margin-right: 3px;
  width: 18px;
  height: 18px;
  line-height: 18px;
  font-size: 8px;
  text-align: center;
  position: relative;
  top: -1px;
}

.zeta-icon-export {
  cursor: pointer;
  font-size: 14px;
  color: #cacbcf;
  margin-left: 5px;
  position: relative;
  top: 2px;
}
.zeta-icon-export:hover {
  color: #4d8cca;
}
</style>
