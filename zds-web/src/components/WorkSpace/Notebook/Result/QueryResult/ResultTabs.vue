<template>
  <div class="result-tabs-wrapper">
    <div class="result-tabs-nav-bar">
      <ul class="result-tabs-action-items x-flex">
        <li>
          <button
            class="left action"
            @click="nextTab"
          />
        </li>
        <li>
          <button
            class="right action"
            @click="previousTab"
          />
        </li>
      </ul>
      <ul class="result-tabs-list x-flex">
        <li
          v-for="result in resultsArr"
          :key="result.jobId"
          :style="{left: tabLeft}"
          :class="{ active: result.jobId == activeIndex }"
        >
          <button
            class="result-tabs-list-li-button"
            @click="clickTab(result.jobId)"
          >
            {{ getTabName(result) }}
          </button>
          <button
            class="close action"
            @click="closeTab(result.jobId)"
          />
        </li>
      </ul>
    </div>
    <keep-alive v-if="!_.isEmpty(results)">
      <result
        :key="activeIndex"
        :notebook="notebook"
        :result="results[activeIndex]"
      />
    </keep-alive>
    <div
      v-else
      class="no-result"
    >
      No Result
    </div>
  </div>
</template>

<script lang="ts">
/**
 * Component <ResultTabs>. Display multiple jobs in navigable tabs.
 *
 * @prop notebook:INotebook Current notebook
 * @prop results:Dict<IJob> Submitted jobs.
 */
import { Component, Vue, Prop, Inject, Watch } from 'vue-property-decorator';
import Result from './Result.vue';
import { INotebook } from '@/types/workspace';
import _ from 'lodash';
import $ from 'jquery';
import { NotebookJob } from '@/types/workspace/notebook-job';
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
@Component({
  components: {
    Result,
  },
})
export default class ResultTabs extends Vue {
  /* Injected at <Notebook> */
  @Inject()
  notebookRemoteService: NotebookRemoteService;
  @Prop() notebook: INotebook;
  get results(): Dict<NotebookJob> {
    if(!this.notebook) return {};
    return _.pick(this.$store.state.workspace.jobs, this.notebook.jobs);
  }
  get resultsArr(): Array<NotebookJob> {
    return _.chain(this.results).values().reverse().value();
  }
  get _() {
    return _;
  }

  get activeIndex() {
    if(!this.notebook) return '';
    if (this.notebook.activeJobId) return this.notebook.activeJobId;
    else {
      /* Initialize activeIndex if not presented */
      if (_.isEmpty(this.results)) return;
      const firstJobId = Object.keys(this.results)[0];
      this.$store.dispatch('setActiveJobById', {
        nid: this.notebook.notebookId,
        jid: firstJobId,
      });
      return firstJobId;
    }
  }
  tabLeft = '0';
  getTabName(result: NotebookJob) {
    const seq = result.seq ? result.seq : 1;
    return `Task ${seq}`;
  }

  clickTab(jid: string) {
    this.$store.dispatch('setActiveJobById', {
      nid: this.notebook.notebookId,
      jid,
    });
  }

  closeTab(jid: string) {
    const result = this.results[jid];
    const latestJob = _.maxBy(_.toArray(this.results), 'seq');
    const isLatest = latestJob && result.jobId === latestJob.jobId;
    this.$store.dispatch('closeJobById', {
      nid: this.notebook.notebookId,
      jobId: jid,
    });

    // clear cache in server if it's latest job
    if (isLatest) {
      this.notebookRemoteService.clearResult(this.notebook.notebookId);
    }
  }

  nextTab() {
    const keys = Object.keys(this.results);
    const ind = keys.indexOf(this.activeIndex as string);
    if (ind < keys.length - 1) {
      this.$store.dispatch('setActiveJobById', {
        nid: this.notebook.notebookId,
        jid: keys[ind + 1],
      });
    }

  }

  previousTab() {
    const keys = Object.keys(this.results);
    const ind = keys.indexOf(this.activeIndex as string);
    if (ind > 0) {
      this.$store.dispatch('setActiveJobById', {
        nid: this.notebook.notebookId,
        jid: keys[ind - 1],
      });
    }

  }
  @Watch('activeIndex')
  private scroll2Active() {
    const ids = Object.keys(this.results).reverse();
    const index = this.activeIndex ? ids.indexOf(this.activeIndex) : -1;
    if(index < 0) {
      return;
    }
    const $tabList = $('.result-tabs-list');
    const $activeTab = $('result-tabs-list li.active');
    const scrollLeft =  - (($tabList.width() || 0) - ((index + 2)* 100));
    if(scrollLeft < 0) {
      this.tabLeft = '0';
      return;
    }
    this.tabLeft = `-${Math.ceil(scrollLeft /100.0) * 100}px`;
  }
}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
$ui-font-size: 13px;
$result-nav-tabs-height: 35px;
$resuilt-tab-list-li-width: 100px;
.result-tabs-wrapper {

    display: flex;
    flex-direction: column;
    height: 100%;

    .right {
        width: 100%;
    }

    .result-tabs-nav-bar {
        height: $result-nav-tabs-height;
        min-height: $result-nav-tabs-height;
        width: 100%;
        background-color: white;
        border-bottom: 1px solid #D7D7D7;
        border-top: 1px solid #D7D7D7;
        position: relative;
        display: flex;
        color: $zeta-global-color-disable;

        $nav-btn-width: 24px;

        button {
            /* cancel out default focus style */
            outline: none;
            user-select: none;
            height: 100%;

            &.action::after {
                font-size: 1.5em;
                position: relative;
                font-weight: 100;
            }

            &.close::after {
                content: "⨯";
                top: -2px;
            }

            &.left:hover,
            &.right:hover {
                color: $zeta-global-color;
                background-color: white;
                cursor: pointer;
            }


            &.right,
            &.left {
                width: $nav-btn-width;
            }

            &.right {
                border-right: 1px solid #D7D7D7;
            }

            &.left::after {
                content: "<";
            }

            &.right::after {
                content: ">";
            }

            &.close::after {
                /* Make close button slightly bigger */
                margin-right: -5px;
                padding-right: 5px;
                padding-left: 3px;
            }
        }

        .result-tabs-action-items {
            height: 80%;
            margin: auto 0;
        }

        .result-tabs-list {
            overflow: hidden;
            bottom: -1px;
            position: relative;
            font-size: $ui-font-size;
            height: 80%;
            margin: auto 0;
            flex-grow: 1;
            li.active {
                color: $zeta-global-color;
            }

            li {
                position: relative;
                min-width: $resuilt-tab-list-li-width;
                border-right: 1px solid #D7D7D7;
                box-sizing: border-box;
                padding: 0px 20px;
                display: flex;

                >.result-tabs-list-li-button {
                    flex: 1 1 auto;
                }

                >.result-tabs-list-li-button:hover {
                    color: $zeta-global-color;
                    cursor: pointer;
                }

                >.close {
                    color: $zeta-global-color-disable;
                }

                >.close:hover {
                    color: $zeta-font-light-color;
                    cursor: pointer;
                }
            }
        }

        ul {
            list-style-type: none;
        }

        .x-flex {
            display: flex;
        }
    }

    .no-result {
        height: calc(100% - #{$result-nav-tabs-height});
        text-align: center;
        display: flex;
        justify-content: center;
        flex-direction: column;
        font-size: 2em;
        color: #ccc;
    }
}
</style>
