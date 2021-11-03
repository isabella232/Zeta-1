<template>
  <div class="nav-metadata">
    <div class="nav-subview-title">
      <div>Metadata Search</div>
      <button
        class="close-btn"
        @click="() => close()"
      >
        <i class="zeta-icon-close" />
      </button>
      <button
        v-click-metric:METADATA_BROWSE="{ name: 'domain list', trigger: 'click' }"
        class="close-btn"
        style="margin-right: 5px;"
        @click="() => browse()"
      >
        <i class="zeta-icon-fullScreen" />
      </button>
    </div>
    <div class="nav-subview-content">
      <div class="searchbox">
        <el-select
          slot="prepend"
          v-model="searchType"
          class="search-type"
          placeholder
          size="mini"
          :popper-append-to-body="(false)"
        >
          <el-option
            v-for="(op,$i) in searchTypeOptions"
            :key="$i"
            :label="op"
            :value="op"
          />
        </el-select>
        <el-input
          v-model="searchContent"
          placeholder
          class="search-input"
          size="mini"
          clearable
        >
          <i
            slot="prefix"
            class="el-input__icon el-icon-search"
          />
          <!-- <i slot="suffix" class="el-input__icon zeta-icon-error" @click="() => clearSearchBox()"></i> -->
        </el-input>
      </div>
      <div class="register">
        <!--span style="margin-right: 10px;" @click="() => browse()">Metadata Browse</span-->
        <span
          v-click-metric:REGISTER_DATASET="{name: 'visit'}"
          @click="() => register()"
        >Register Dataset</span>
      </div>
      <div
        v-show="metaShow"
        v-loading="metaLoading"
        class="meta-display"
      >
        <ul v-if="metaList && metaList.length > 0">
          <li
            v-for="(meta,$index) in metaList"
            :key="$index"
            v-click-metric:METADATA_TABLE_DETAIL="{name: 'overview', trigger: 'nav_metadata_search_click', table: meta.name }"
            class="meta-item"
            @click="() => openMetaTab(meta)"
          >
            <div class="meta-info">
              <div class="meta-name">
                {{ meta.name }}<span
                  v-if="isVDM(meta)"
                  class="vdm"
                >VDM</span>
              </div>
              <div
                class="meta-desc"
                :class="{'na':!meta.desc}"
              >
                {{ meta.desc ? meta.desc : 'N/A' }}
              </div>
            </div>
            <div class="meta-platform">
              <i
                v-for="(p,$i) in meta.platformOptions"
                :key="$i"
                :title="p"
                :class="p.toLowerCase()"
                class="platform-icon"
              />
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
      <MetadataUDF v-show="!metaShow" />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch, Inject } from 'vue-property-decorator';
import MetadataUDF from './MetadataUDF.vue';
import { WorkSpaceType, IMetadata, platform, Source } from '@/types/workspace';
import _ from 'lodash';
import MetadataService from '@/services/Metadata.service';
import { WorkspaceSrv } from '@/services/Workspace.service';
import * as Logger from '@/plugins/logs/zds-logger';
function metaMapper (metaList: Source[]) {
  return metaList.map(m => {
    const plat = _.chain(m.platformOptions).map(p => {
      if (p === 'Mozart') {
        return undefined;
      } else {
        return p;
      }
    }).compact().value();
    m.platformOptions = plat;
    return m;
  });
}
@Component({
  components: {
    MetadataUDF,
  },
})
export default class NavMetadata extends Vue {
  @Inject()
  metadataService: MetadataService;

  readonly searchTypeOptions = ['Table']; //['Table', 'Query'];
  private searchType: string;
  private searchContent = '';

  // get metaShow():boolean{
  // 	return Boolean(this.searchContent)
  // }
  readonly metaShow = true;
  private metaLoading = true;
  private metaList: Source[] = [];

  private debounceSearch: Function;
  get reload (): boolean {
    return this.$store.getters.getMetadataReload;
  }

  constructor () {
    super();
    this.searchType = this.searchTypeOptions[0];
    this.debounceSearch = _.debounce(this.metadataSearchHandler, 500);
  }
  mounted () {
    this.metadataSearchHandler('');
  }
  clearSearchBox () {
    this.searchContent = '';
  }

  close () {
    this.$emit('navSubViewClose');
  }
  metadataSearchHandler (queryStr: string = this.searchContent) {
    console.debug('metadata search ' + queryStr);
    this.metaList = [];
    this.metaLoading = true;
    if (!_.isEmpty(queryStr)) Logger.counter('METADATA_SEARCH', 1, { name: 'nav_search' });
    this.metadataService.queryMetadataList(queryStr).then(metas => {
      this.metaList = metaMapper(metas as Source[]);
      this.metaLoading = false;
      this.$store.dispatch('setMetadataReload', false);
    });
  }
  openMetaTab (meta: Source) {
    const ws = WorkspaceSrv.metadata(meta);
    //this.$store.dispatch("addActiveWorkSpace", ws);
    //this.$router.push("/notebook");
    //this.$store.dispatch("setMetadataSearch", ws);
    const table: any = '/metadata?tableName=' + ws.name;
    const type: any = meta.rowType && _.indexOf(meta.rowType, 'vdm') > -1 ? '&&type=vdm' : '';
    this.$router.push(table + type);
    this.close();
  }
  isVDM (meta: any) {
    if (meta.rowType && _.indexOf(meta.rowType, 'vdm') > -1) {
      return true;
    }
    return false;
  }
  register () {
    this.$router.push('/registervdm');
    this.close();
  }
  browse () {
    this.$router.push('/metadata');
    this.close();
  }
  formatTitle (p: platform): string {
    const title = p.toString();
    return title.substring(0, 1).toUpperCase() + title.substring(1);
  }
  @Watch('searchContent')
  onSearchChange (txt: string) {
    this.debounceSearch(txt);
  }
  @Watch('reload')
  onReload (val: boolean) {
    if (val) {
      this.debounceSearch(this.searchContent);
    }
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.nav-metadata {
    width: 450px;
    padding: 15px 15px;
    font-family: "Helvetica-Bold", "Helvetica Bold", "Helvetica";
    .nav-subview-title {
        width: 100%;
        // display: flex;
        // justify-content: space-between;
        // align-items: center;
        padding: 10px 0;
        > div {
            font-size: 20px;
            display: inline;
        }
        > .close-btn {
            color: $zeta-font-color;
            cursor: pointer;
            display: inline;
            float: right;
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
        .searchbox {
            display: flex;
            padding: 5px 15px;
            > .search-type {
                max-width: 90px;
            }
            > .el-input.search-input {
                width: auto;
                flex-grow: 1;
                /deep/ .zeta-icon-error {
                    color: #ccc;
                    cursor: pointer;
                    &:hover {
                        color: $zeta-global-color-heighlight;
                    }
                }
            }
        }
        .register {
            color: #569CE1;
            padding: 0px 15px;
            text-align: right;
            text-decoration: underline;
            > span {
                cursor: pointer;
            }
            > span:hover {
                 color: #4D8CCA;
            }
        }
        .browse {
            color: #569CE1;
            padding: 0px 15px;
            text-align: right;
            text-decoration: underline;
            > span {
                cursor: pointer;
            }
            > span:hover {
                 color: #4D8CCA;
            }
        }
        .meta-display {
            height: calc(100% - 53px);

            > ul {
                height: 100%;
                list-style-type: none;
                overflow: auto;
                > li.meta-item {
                    cursor: pointer;
                    display: flex;
                    // justify-content: space-between;
                    flex-direction: column;
                    padding: 5px 0px;
                    border-bottom: 1px solid #ccc;
                    &:hover {
                        color: $zeta-global-color-heighlight;
                        .meta-info {
                            display: flex;
                            flex-direction: column;
                            // .meta-name {
                            // }
                            .meta-desc {
                                color: inherit;
                            }
                        }
                    }
                    .meta-info {
                        display: flex;
                        flex-direction: column;
                        .meta-name {
                            display: flex;
                            /deep/ .vdm {
                                color: #cacbcf;
                                font-family: "italic";
                                font-size: 10px;
                                margin: auto 5px;
                            }
                        }
                        .meta-desc {
                            font-size: 12px;
                            color: #999;
                            text-overflow: -o-ellipsis-lastline;
                            overflow: hidden;
                            text-overflow: ellipsis;
                            display: -webkit-box;
                            -webkit-line-clamp: 2;
                            -webkit-box-orient: vertical;
                            height: 28px;
                            font-family: $font-default;
                            &.na {
                                font-size: 18px;
                                color: #ccc;
                                -webkit-line-clamp: 1;
                                line-height: 28px;
                                text-align: center;
                            }
                        }
                    }
                    .meta-platform {
                        // width: 120px;
                        // flex-shrink: 0;
                        margin-top: 5px;
                        justify-content: flex-end;
                        display: flex;
                        align-items: center;
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

