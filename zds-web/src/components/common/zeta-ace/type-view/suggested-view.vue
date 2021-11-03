<template>
  <div>
    <ul class="result-list" :class="showMore(resultHTML)?'showMore':''">
      <div v-for="result in resultHTML" :key="result.searchType">
        <li
          v-for="(item, index) in result.slice(0,getSlice(result.length))"
          :key="index"
          :class="getTypeClass(item.searchType)"
        >
          <i
            v-if="item.searchType"
            :class="item.searchType"
            class="type"
          >{{ (item.searchType === 'Search'||item.searchType === 'New')?'?':item.searchType }}</i>
          <span 
            v-click-metric:ACE_CLICK="{name: 'getAnswer => '+item.searchType}"
            v-html="item.nameHTML"
            @click="clickHint(item)"
          />
          <span v-if="item.searchType === 'FAQ'">
            <i v-for="(tag, i) in item.tags" :key="i" class="tag">#{{ tag.name }}</i>
          </span>
          <div v-if="item.searchType === 'Table'" class="platform">
            <i v-for="(p, i) in item.platform" :key="i" :class="getIconClass(p)" class="platform-icon workspace-icon" />
          </div>
          <span v-if="item.searchType === 'Notebook'" class="platform">
            <i :class="getIconClass(item.platform)" class="platform-icon workspace-icon" />
          </span>
        </li>
      </div>
    </ul>
    <div v-if="showMore(resultHTML)" @click="toggle" class="show-more">
      <span v-if="sliceCount===2">Expand more results<i class="el-icon-caret-bottom" /></span>
      <span v-else>Collapse<i class="el-icon-caret-top" /></span>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import _ from 'lodash';
import { getPlatformIconClass, openNotebookTab } from '../utilties';
@Component({
  components: {}
})
export default class SuggestedView extends Vue {
  @Prop() resultHTML: Dict<any>;
  sliceCount = 2;

  toggle(){
    this.sliceCount>0?this.sliceCount=-1:this.sliceCount=2;
  }
  showMore(result: any){
    if(_.keys(result).length===0) return false;
    return result.faq.length>2||result.table.length>2||result.udf.length>2?true:false;
  }
  getSlice(len: number){
    return this.sliceCount>0?this.sliceCount: len;
  }
  clickHint(item: any){
    if(item.searchType === 'Notebook'){
      openNotebookTab(item);
      return;
    }
    this.$emit('click-item', item);
  }
  getIconClass(platform: string){
    return getPlatformIconClass(platform);
  }
  getTypeClass(klass: string){
    return _.lowerCase(klass);
  }
}
</script>
<style lang="scss" scoped>
.tag{
  font-style: normal;
  color: #569ce1;
  margin-left: 10px;
}
</style>
