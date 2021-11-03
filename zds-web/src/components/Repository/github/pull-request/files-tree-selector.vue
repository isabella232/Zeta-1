<template>
    <div class="file-selector selector" v-loading="loading">
        <el-tree ref="tree" :props="defaultProps" lazy :load="loadSubFiles" node-key="id">
            <span class="custom-tree-node" slot-scope="{ node, data }">
                <el-checkbox v-if="data.type !== 'FOLDER'" v-model="data.selected" @change="onCheckboxSelected">{{ node.label }}</el-checkbox>
                <template v-else>
                    <i  class="icon-folder-empty"/>
                    <span>{{ node.label }}</span>
                </template>
            </span>
        </el-tree>
    </div>    
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Emit, Inject } from "vue-property-decorator";
import GitRemoteService from '@/services/remote/GithubService'
import Util from "@/services/Util.service";
import _ from 'lodash';
import { TreeNode } from "element-ui/types/tree";
import { GithubFile, FileSelector } from '.';
function parseFiles(src:Partial<GithubFile>[], currentPath: string = ''): GithubFile[]{
    return _.map(src,(srcFile:Partial<GithubFile>) => {
        const newFile = {
            fullPath: ( currentPath ?  currentPath + '/' : '' ) + srcFile.path,
            path: srcFile.path || '',
            type: srcFile.type || 'FILE_NORMAL',
            sha: srcFile.sha || '',
            selected: false,
            // subFiles: [] as GithubFile[]
        }
        return newFile
    })
}
function filterNodes(nodes:TreeNode<any,GithubFile>[]):GithubFile[] {
    // return _.chain(nodes).map((node) => filterNode(node)).value()
    let result:GithubFile[] = [];
    _.forEach(nodes, (node) => {
        let subResult = filterNode(node)
        result = _.concat(result, subResult)
    })
    return result
}
function filterNode(node:TreeNode<any,GithubFile>){
    let result:GithubFile[] = [];
    const data:GithubFile = node.data;
    if(data && data.type == 'FILE_NORMAL'){
        // return data.selected ? data : undefined
        if(data.selected){
            result.push(data)
        }
    }
    else{
        const subResult = filterNodes(node.childNodes)
        result = _.concat(result, subResult)
    }
    return result
}
@Component({
  components: {
  }
})
export default class FilesTreeSelector extends Vue implements FileSelector{
    @Inject('gitRemoteService')
    gitRemoteService: GitRemoteService

    @Prop()
    url: string
    @Prop()
    branch: string 
    loading = false;
    readonly defaultProps = {
        label:'path',
        children: 'subFiles',
        isLeaf: (file: GithubFile) =>  file.type !== 'FOLDER'
    }
    get $node():TreeNode<any,GithubFile>{
        return (this.$refs.tree as any).root
    }

    loadSubFiles(node:any, resolve: (subfiles: GithubFile[]) => void){
        if(node.level === 0){
            this.loading = true;
            this.gitRemoteService.fetchFiles(Util.getNt(),this.url,this.branch)
            .then(({data}: {data :Partial<GithubFile>[]}) => {
                resolve(parseFiles(data))
                this.loading = false;
            })
            .catch((e) => {
                e.message = 'load files failed'
                this.loading = false;
            })
        }
        else{
            const fileData: GithubFile = node.data; 
            const currentPath = fileData.fullPath
            this.gitRemoteService.fetchFiles(Util.getNt(),this.url,this.branch,fileData.sha)
            .then(({data}: {data :Partial<GithubFile>[]}) => {
                resolve(parseFiles(data,currentPath))
                this.loading = false;
            })
            .catch((e) => {
                e.message = 'load files failed'
                this.loading = false;
            })
        }
    }
    getSelectedNodes(){
        return filterNode(this.$node)
    }

    clear(){
        (this.$refs.tree as any).remove(this.$node)
    }
    onCheckboxSelected(){
        const selectedNodes = this.getSelectedNodes();
        this.$emit('onSelected',selectedNodes.length)
    }
}
</script>
<style lang="scss" scoped>

</style>
