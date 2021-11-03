<template>
  <div>
    <span
      class="content-name"
      @click="jump"
    >
      {{ data.name }}
    </span>
    <i class="query type-icon" />
    <i
      class="icon zeta-icon-export"
      @click="jump"
    />

    <div class="content-desc">
      <div style="color:#999">
        {{ extra.executedCnt }} runtimes by {{ extra.nt }}
      </div>
      <div style="color:#999">
        <span>
          Updated at: {{ lastUpdate }}
        </span>
        <span v-if="lastRun">
          Last run at: {{ lastRun }}
        </span>
      </div>
      <div>
        {{ data.desc }}
      </div>
    </div>
    <div>
      <i
        :class="platform.toLowerCase()"
        class="platform-icon"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import moment from 'moment';
import Util from '@/services/Util.service';

@Component({})
export default class Notebook extends Vue {

  @Inject() apmTransaction: any;
  @Prop() data: any;
  @Prop() pos: any;
  @Prop({ default: () => ({}) }) extra: any;

  owner = '';

  get preference() {
    return this.extra.preference ? JSON.parse(this.extra.preference) : {};
  }

  get platform() {
    const connection = this.preference['notebook.connection'] || {};
    let platform = connection['alias'];
    if(!platform){
      platform = Util.getDefaultConnectionByPlat(connection['codeType']).defaultConnection.alias;
    }
    return platform;
  }

  get lastUpdate() {
    if (this.extra.lastUpdateDt) {
      return moment(this.extra.lastUpdateDt).format('YYYY-MM-DD HH:mm');
    }
  }

  get lastRun() {
    if (this.extra.lastRunDt) {
      return moment(this.extra.lastRunDt).format('YYYY-MM-DD HH:mm');
    }
  }

  jump() {
    const transaction = this.apmTransaction('metadata', 'view', this.pos);
    setTimeout(() => {
      transaction && transaction.end();
      const { type, id } = this.data.parsedContent._source;
      const url = type === 'zepplin' ? `/zeta/share/#/notebook/multilang?notebookId=${id}` : `/zeta/share/#/notebook?notebookId=${id}`;
      window.open(location.origin + url);
    }, 10);
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/metadata.scss';
span.content-name {
  text-transform: none;
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
