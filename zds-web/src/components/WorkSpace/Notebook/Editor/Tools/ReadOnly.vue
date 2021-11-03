<template>
    <div class="readonly-popup">
        <span class="left info">
            <i class="zeta-icon-info"/>
            <template v-if="offline">
                Lost connection to server <el-button type="text" @click="refreshPage">refresh page</el-button>
            </template>
            <template v-else>
                {{message}}
            </template>
        </span>
        <!-- <span class="right close">
            <i class="zeta-icon-close" @click="show = false"/>
        </span> -->
    </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from "vue-property-decorator";
import { Status } from '@/net/ws';
import _ from "lodash";
import Util from "@/services/Util.service";
@Component({
  components: {}
})
export default class ReadOnly extends Vue {
    @Prop()
    connectionStatus: Status;
    @Prop({ default: 'This notebook is READ ONLY'}) message;

    get offline() {
        if(Util.isShareApp()) {
            return false;
        }
        if(this.connectionStatus){
            return (this.connectionStatus != Status.LOGGED_IN)
        } else {
            return false
        }

    }
    refreshPage(){
        Util.reloadPage();
    }

}
</script>

<style lang="scss" scoped>
$color:#EBB563;
@import '@/styles/global.scss';
.readonly-popup{
    background-color: #FDF7E9;
    display: flex;
    justify-content: space-between;
    padding: 5px 10px;
    border-left: 1px solid #dddddd;
    border-right: 1px solid #dddddd;
    span.info{
        &,
        i{
            color: $color;
        }
    }
    span.close{
        &i {
            &:hover{
                color:$zeta-global-color;
            }
        }
    }


}
</style>
