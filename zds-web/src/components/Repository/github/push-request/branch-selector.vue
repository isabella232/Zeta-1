<template>
    <div class="zeta-dropdown">
        <el-select :value="$branchDisplay" @input="val => emitValue(val)" :disabled="$disabled" filterable>
            <div class="zeta-dropdown-create">
                <el-input class="new-item" 
                    :class="{'error':newBranchExist}"
                    :value="$newBranch" @input="val => emitNewBranch(val)" 
                    placeholder="New Branch" clearable>
                </el-input>
                <div v-if="newBranchExist" class="zeta-dropdown-create-error"><i class="el-icon-warning"/>branch already exist</div>
            </div>
            <el-option v-for="(b,$index) in branches" :key="$index" :label="b" :value="b">{{b}}</el-option>
        </el-select>
        <template v-if="$newBranch && !newBranchExist">
            base on <strong>'{{$branch}}'</strong>
        </template>
    </div>    
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Emit } from "vue-property-decorator";
import _ from 'lodash';

@Component({
  components: {
  }
})
export default class BranchSelector extends Vue {
    @Prop({type:Boolean, default: false})
    disabled: boolean
    get $disabled(){
        return this.disabled
    }

    @Prop({type:Array,default:[]})
    branches: string[]
    get $branches(){
        return this.branches
    }

    @Prop({type: String, default: ''})
    value: string
    get $branch(){
        return this.value
    }

    @Prop({type: String, default: ''})
    newBranch: string
    get $newBranch(){
        return this.newBranch
    }

    get $branchDisplay(){
        if(this.newBranchExist){
            return this.$branch
        }
        return this.newBranch ? `${this.newBranch}` : this.$branch
    }
    get newBranchExist() {
        return _.includes(this.$branches, this.newBranch)
        // return this.$branches.includes(this.newBranch)
    }
    @Emit('input')
    emitValue(b: string = this.$branch){
        return b
        
    }

    @Emit('emitNewBranch')
    emitNewBranch(b: string){
        return b
    }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.zeta-dropdown-create{
    text-align: center;
    
    .new-item{
        margin: 5px 0;
        width: calc(100% - 15px);
    }
    .el-input.error{
        /deep/ .el-input__inner{
            border:1px solid $zeta-global-color-red;
        }
    }
    .zeta-dropdown-create-error{
        padding-left: 10px;
        text-align: left;
        font-size: 12px;
        color: $zeta-global-color-red;
        margin-top: 5px;
        margin-bottom: 5px;
        display: flex;
        align-items: center;
    }

}
.zeta-dropdown {
 /deep/ .el-input {
        height: 30px;
        > input {
            height: 30px !important;
        }
    }
}
</style>
