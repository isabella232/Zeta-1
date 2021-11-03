<template>
    <div class="zeta-dropdown">
        <el-select :value="selected" @input="emitValue" >
            <div class="zeta-dropdown-create">
                <el-input class="new-item" v-model="newItem" placeholder="New Item" :disabled="newItemCreating">
                </el-input>
                <el-button class="add-item circle-btn" size="mini" circle type="text" :disabled="newItem === ''" @click="createNewItem()" :loading="newItemCreating"><i class="el-icon-plus"/></el-button>
            </div>
            <el-option v-for="(opt,$index) in dropDownOptions" :key="$index" :label="opt.label" :value="opt.value">{{(opt && opt.isNew)? '*':''}}{{opt.value}}</el-option>
        </el-select>
    </div>    
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Emit } from "vue-property-decorator";
import _ from 'lodash';
interface DropdownOption{
    label?: string
    value: string
    isNew?: boolean 
}

function getOptionsType(opts: any[]){
    if(!opts || !opts[0]) return null
    const val = opts[0]
    if(typeof val === 'string'){
        return 'array';
    } else {
        return _.has(val,'value') ? 'object' : null
    }
}
function parseOptions(opts: any[]){
    const type = getOptionsType(opts)
    if(type === null){
        throw 'cannot resolve input';
        
    } else if( type === 'array'){
        return opts.map(opt => {
            return <DropdownOption>{
                label: opt,
                value: opt
            }
        })
    }
    else if( type === 'object'){
        opts.map((opt: DropdownOption) => {
            const value = opt.value
            const label = opt.label ? opt.label : opt.value
            return <DropdownOption>{
                label: label,
                value: value
            }
        })
    }
}
@Component({
  components: {
  }
})
export default class ZetaDropdown extends Vue {
    @Prop()
    value: string 
    get selected(){
        return this.value
    }
    @Emit('input')
    emitValue($val: string){
        // this.value = $val
    }

    @Prop()
    options:any[] | DropdownOption[] 
    t_optionType: 'object' | 'array' | null = null;
    dropDownOptions: DropdownOption[] = [];
    newItem:string = ''
    newItemCreating: boolean = false;
    mounted(){
        this.parseOptions(this.options)
    }
    parseOptions(opts:any[]){
        try {
            const options = parseOptions(opts);
            this.dropDownOptions = options ? options : [];
            this.t_optionType = getOptionsType(opts)
        }
        catch(e) {
            this.dropDownOptions = []
            this.t_optionType = 'object'
            console.error(e)
        }
    }

    createNewItem(){
        const name = this.newItem;
        const newOption =<DropdownOption>{
            label: name,
            value: name,
            isNew: true
        }
        if(!this.dropDownOptions){
            this.dropDownOptions = [newOption]
        } else {
        this.dropDownOptions.push(newOption);
        }
        this.onCreateItem(this.t_optionType === 'array' ? name : newOption)
        this.newItem = ''
    }

    @Emit('onCreateItem')
    onCreateItem(item: string | DropdownOption){

    }
    @Watch('options')
    onOptionsChange(newVal:any[]){
        this.parseOptions(newVal)
    }


}
</script>
<style lang="scss" scoped>
.zeta-dropdown-create{
    padding-left: 5px;
    margin-bottom: 5px;
    .el-input.new-item{
        width: calc(100% - 40px)
    }
    .add-item{
        margin-left: 5px;
    }

}

</style>
