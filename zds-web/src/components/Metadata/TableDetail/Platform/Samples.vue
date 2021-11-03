<template>
  <div class="samples">
    <div 
      v-for="(q, $i) in queriesByPlatform"
      :key="q.query_text"
      class="sample"
    >
      <header>
        <h1>{{ q.title || 'Smart Query ' + $i }}</h1>
        <div>
          <span
            v-if="q.sample_query_id === undefined"
            class="smart-icon"
          >
            Engine by Smart Query
          </span>
          <i
            v-for="p in (q.platform || q.platforms).split(',')"
            :key="p"
            class="platform-icon"
            :class="p.toLowerCase()"
          />
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
            <i
              class="el-icon-question"
              style="color:#aaa"
            />
          </el-tooltip>
          <div class="logo-icon">
            <img :src="'./img/logo.png'">
            <span style="vertical-align:2px;">ZETA</span>
          </div>
        </div>
      </header>
      <SqlEditor
        :value="formatSql(q.query_text)"
        :options="{ lineNumbers: false }"
        read-only
      />
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import SqlEditor from '@/components/common/Visualization/CodeDisplay.vue';
import Util from '@/services/Util.service';
import { Action, State } from 'vuex-class';
import { Actions } from '../store';
import { matchString } from '../../utils';
@Component({
  components: {
    SqlEditor,
  },
})
export default class Samples extends Vue {
  @Action(Actions.GetSampleQueries) getSampleQueries;
  @State(state => state.TableDetail.queries) queries;
  @Prop() table: string;
  @Prop() platform: string;

  get queriesByPlatform () {
    return this.queries.filter(q => matchString(this.platform, q.platforms) || matchString(this.platform, q.platform));
  }
  @Watch('table', { immediate: true })
  async getQueries (table) {
    if (!table) return;
    this.getSampleQueries(table);
  }
  formatSql (text) {
    return Util.SQLFormatter(text);
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/metadata.scss';

.samples {
  padding: 0 20px;
  .sample {
    border-top: 1px solid #EBEEF5;
    padding: 12px 0 24px;
    header {
      display: flex;
      justify-content: space-between;
      h1 {
        color: rgba(0, 0, 0, 0.65);
        font-size: 14px;
      }
      .smart-icon {
        display: inline-block;
        background-color: rgba(92, 184, 92, 1);
        font-size: 10px;
        text-align: center;
        padding: 0 5px;
        color: #FFF;
        height: 15px;
        line-height: 15px;
        border-radius: 4px;
      }
      .logo-icon {
        display: inline-block;
        color: #00A5C5;
        vertical-align: sub;
        img {
          height: 15px;
          width:15px;
        }
      }
    }
  }
}
</style>