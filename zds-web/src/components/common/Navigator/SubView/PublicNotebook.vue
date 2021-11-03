<template>
  <div class="nav-public-notebook">
    <div class="nav-subview-title">
      <div>Public Notebook</div>
      <button
        class="close-btn"
        @click="() => close()"
      >
        <i class="zeta-icon-close" />
      </button>
    </div>
    <div class="nav-subview-content">
      <!-- <div class=searchbox>
				<el-select v-model="searchType" class=search-type slot="prepend" placeholder="" size="mini" :popper-append-to-body="(false)">
					<el-option  v-for="(op,$i) in searchTypeOptions" :label="op" :value="op" :key="$i" />
				</el-select>
				<el-input placeholder="" v-model="searchContent" class=search-input size="mini"  clearable >
	
					<i slot="prefix" class="el-input__icon el-icon-search"></i>
				</el-input>
            </div>-->
      <div
        v-loading="reportsLoading"
        class="nbs-display"
      >
        <ul v-if="reports && reports.length > 0">
          <li
            v-for="(report,$index) in reports"
            :key="$index"
            v-click-metric:PUBLICK_NOTEBOOK_CLICK="{name: 'open'}"
            class="nb-item"
            @click="() => noteClickHandler(report.id, report)"
          >
            <div class="nb-name">
              {{ report.title }}
            </div>
          </li>
        </ul>
        <div
          v-else
          class="no-data"
        >
          No Data
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Inject } from "vue-property-decorator";
import {
  WorkSpaceType,
} from "@/types/workspace";
import NotebookRemoteService from '@/services/remote/NotebookRemoteService';
import _ from "lodash";
import { WorkspaceSrv } from "@/services/Workspace.service";
import Util from "@/services/Util.service";
import { ZetaException } from "@/types/exception";
import { IFileMapper } from "@/services/mapper";
import ZeppelinApi from "@/services/remote/ZeppelinApi";
import moment from 'moment';
interface PubicNotebook {
    id: string;
    title: string;
    nt: string;
    isZeppelin?: boolean;
}
const zeppelinNotes: PubicNotebook[] = [
  {
    id: '2EW7XGGQ2',
    title: 'Top keyword for holiday season',
    nt: 'zhouhuang',
    isZeppelin: true
  },
  {
    id: '2F13Z6YZV',
    title: 'name convention',
    nt: 'zhouhuang',
    isZeppelin: true
  }
];
@Component({
  components: {}
})
export default class PublicNotebook extends Vue {
    private reportsLoading = true;
    private reports: PubicNotebook[] = [];

    @Inject('notebookRemoteService')
    notebookRemoteService: NotebookRemoteService;
    @Inject('zeppelinApi')
    zeppelinApi: ZeppelinApi;
    //   private debounceSearch: Function;

    constructor() {
      super();
    }
    async mounted() {
      this.reportsLoading = true;
      // const nbs = (await this.notebookRemoteService.getAllPublicNotebook())
      //   .data as PubicNotebook[];
      this.loadPublicNotes().then(nbs => {
        this.reportsLoading = false;
        this.reports = nbs;
      });
      
      // this.reports = nbs.concat(zeppelinNotes);
    }
    loadPublicNotes() {
      const zetaReq = this.notebookRemoteService.getAllPublicNotebook();
      const zplReq = this.zeppelinApi.getPublicNotes();
      return Promise.all([zetaReq, zplReq]).then((ress) => {
        const zetaNbs = ress[0].data as PubicNotebook[];
        const zplNbs = ress[1].data.body as any[];
        const nbs: PubicNotebook[] = zetaNbs;
        return nbs.concat(zplNbs.map(nb => {
          return {
            id: nb.id,
            title: nb.title,
            nt: '',
            isZeppelin: true
          };
        }));
      });
    }
    noteClickHandler(id: string, srcData: PubicNotebook) {
      if (srcData.isZeppelin) {
        this.openZplPublicNote(id, srcData);
      } else {
        this.openPublicReports(id, srcData);
      }
    }
    openPublicReports(id: string, srcData: PubicNotebook) {
      const openWorkspace = () => this.$router.push("/notebook");
      const createNB = (data: any) => {
        const nb = WorkspaceSrv.file2nb(data);
        this.$store.dispatch("addActiveWorkSpace", nb);
        this;
      };
      const createFile = (data: any) => {
        const file = IFileMapper.packetMapper(data);
        this.$store.dispatch("addFile", file);
      };
      this.reportsLoading = true;
      if (srcData.nt == Util.getNt()) {
        createNB(srcData);
        this.reportsLoading = false;
        openWorkspace();
        this.close();
        return;
      }
      this.notebookRemoteService
        .getPublicNotebook(id)
        .then(({ data }) => {
          createNB(data);
          this.reportsLoading = false;
          openWorkspace();
          this.close();
        })
        .catch((err: ZetaException) => {
          if (err.code === "ENTITY_IS_NULL") {
            err.resolve();
            this.createNewPublic(id).then(({ data }) => {
              createNB(data);
              createFile(data);
              this.reportsLoading = false;
              openWorkspace();
              this.close();
            });
          } else {
            this.reportsLoading = false;
          }
        });
    }
    createNewPublic(id: string) {
      return this.notebookRemoteService.createPublicNotebook(id);
    }
    openZplPublicNote(id: string, srcData: PubicNotebook): any {
      this.reportsLoading = true;
      const cloneName = '/public_notebooks/'+ srcData.title + '_Clone';
      this.zeppelinApi.publish(id, {name: cloneName}).then((res) => {
        const notebookId = res.data.body;
        const ws = WorkspaceSrv.notebook({
          notebookId,
          name: srcData.title + '_Clone',
          type: WorkSpaceType.NOTEBOOK_ZEPPELIN
        });
        this.$store.dispatch('addActiveWorkSpace', ws);
        this.$router.push({path: '/notebook'});
        const file = IFileMapper.zeppelinNoteMapper({
          name: cloneName,
          id: notebookId,
          opened: 1,
          interpreterSettings: [],
          seq: -1,
          createDt: moment().valueOf()
        });
        this.$store.dispatch("addFile", file);
        this.reportsLoading = false;
        this.close();
      }).catch(() => {
        this.reportsLoading = false;
      });
    }
    close() {
      this.$emit("navSubViewClose");
    }

}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.nav-public-notebook {
    width: 450px;
    padding: 15px 15px;
    font-family: "Helvetica-Bold", "Helvetica Bold", "Helvetica";
    .nav-subview-title {
        width: 100%;
        display: flex;
        justify-content: space-between;
        // align-items: center;
        padding: 10px 0;
        margin-bottom: 40px;
        > div {
            font-size: 20px;
        }
        > .close-btn {
            color: $zeta-font-color;
            cursor: pointer;
            &:hover {
                color: $zeta-font-light-color;
            }
            > i {
                font-size: 18px;
                color: inherit;
            }
        }
    }
    .nav-subview-content {
        height: calc(100% - 43px);

        .nbs-display {
            height: calc(100% - 53px);

            > ul {
                height: 100%;
                list-style-type: none;
                overflow: auto;
                > li.nb-item {
                    cursor: pointer;
                    display: flex;
                    // justify-content: space-between;
                    flex-direction: column;
                    padding: 5px 0px;
                    height: 30px;
                    line-height: 30px;
                    font-size: 18px;
                    color: #666;
                    font-family: "ArialMT", "Arial";
                    &:hover {
                        color: $zeta-global-color-heighlight;
                    }
                }
            }
            > div.no-data {
                font-size: 24px;
                text-align: center;
                margin-top: 30px;
            }
            /deep/ .el-loading-mask {
                background-color: #f4f7f9;
            }
        }
    }
}
</style>

