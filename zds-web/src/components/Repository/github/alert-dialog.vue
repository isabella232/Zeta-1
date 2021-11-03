<template>
    <el-dialog :visible.sync="visible" :close-on-click-modal="false">
        <template slot="title">
            <div class="title">
                <i class="zeta-icon-info"/>
                <span>Github Error</span>
            </div>

        </template>
        <div>
            {{msg}}
        </div>
        <span slot="footer" class="dialog-footer">
            <!-- <el-button type="primary" @click="goSetting">Set Token</el-button> -->
            <el-button plain @click="visible = false">Close</el-button>
        </span>
    </el-dialog>
</template>
<script lang="ts">
import { Component, Vue, Prop, Watch, Emit } from "vue-property-decorator";
import _ from 'lodash';

@Component({
  components: {
  }
})
export default class AlertDialog extends Vue {
    @Prop({type:Boolean, default: false})
    value: boolean
    get visible(){
        return this.value
    }
    set visible(v: boolean){
        this.setVisible(v)
    }


    @Prop({type: String, default: ''})
    message: string

    get msg(){
        return this.message
    }

    @Emit('input')
    setVisible(visible: boolean){

    }
    goSetting(){
        this.$router.push('settings')
        this.visible = false
    }

}
</script>
<style lang="scss" scoped>
.title{
    display: flex;
    i{
        color:red;
        align-self: center;
        margin: 0px 15px;
        font-size: 25px;
    }
    span{
        font-weight: 700;
        font-family: 'Arial-BoldMT', 'Arial Bold', 'Arial';
        font-size: 16px;
    }
}
</style>
