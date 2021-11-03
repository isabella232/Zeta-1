<template>
    <div class="file-selector selector" v-loading="loading">
        <ul v-if="filesList && filesList.length > 0">
            <li class="file-item" v-for="(f,$i) in filesList" :key="$i">
                <slot name="item" :item="f">
                    <template v-if="f.type !== 'FOLDER' && showCheckbox">
                        <el-checkbox  v-model="f.selected" @change="onSelected">{{f.path}}</el-checkbox>
                    </template>
                    <template v-else>
                        {{f.path}}
                    </template>
                </slot>
                
            </li>
        </ul>
        <div class="no-data" v-else>
            No Data
        </div>
    </div>    
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Emit } from "vue-property-decorator";
import Util from "@/services/Util.service";
import _ from 'lodash';
import { GithubFile, FileSelector } from '.';

@Component({
  components: {
  }
})
export default class FilesListSelector extends Vue implements FileSelector{


    @Prop({default:false})
    loading = false;
    @Prop({type:Boolean, default: true})
    selectable: boolean
    get showCheckbox(){
        return this.selectable
    }
    @Prop()
    value: any[]  
    get filesList (){
        return this.value
    }
    onSelected(){
        this.onValueChange(this.value)
    }
    @Emit('input')
    onValueChange(v: any[]){
    }
    getSelectedNodes(){
        return _.filter(this.value,file => file.selected as boolean)
    }

    clear(){
        _.forEach(this.filesList, file => file.selected = false)
    }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.file-selector{
    border-top: 1px solid $zeta-global-F2 !important;
    .no-data {
        min-height: 200px;
        font-size: 20px;
        font-weight: 800;
        display: flex;
        justify-content: center;
        align-items: center;
    }
    .file-item {
        border-bottom: 1px solid $zeta-global-F2;
        list-style-type:none;
        padding-left: 10px;
        line-height: 30px;
        font-size: 14px;
    }
}

</style>
