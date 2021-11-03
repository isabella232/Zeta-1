<template>
  <div class="dag-container">
      <!-- v-show="active == 0" -->
      <!-- v-show="active == 1" -->
      <iframe ref="ifr_0"  :class="{'active': active == 0}" class="dag-iframe dag_0" :srcdoc="doc0"/>
      <iframe ref="ifr_1"  :class="{'active': active == 1}" class="dag-iframe dag_1" :srcdoc="doc1"/>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import {getSrcDom} from './utilities';
@Component({
  components: {
  },
})
export default class DAGContainer extends Vue {
    @Prop()
    dagSrc!:string
    iframeLoading:boolean = false;
    active: 0 | 1 = 0;
    doc0:any = null;
    doc1:any = null;

    mounted(){
        let defaultOnload = () => {
            // console.log('tianrsun--','defaultOnload')
            this.iframeLoading = false;
        }
        let currentIframe = this.$refs.ifr_0 as any;
        let iframe = this.$refs.ifr_1 as any;
        currentIframe.onload = this.iframeOnload
        iframe.onload = this.iframeOnload1
        this.doc0 = getSrcDom(this.dagSrc);
        
    }
    /**
     * set inactive iframe src
     */
    @Watch('dagSrc')
    setNewSrc(newSrc:string){
        // console.log('tianrsun--',`on src change`)
        if(this.iframeLoading){
            // console.log('tianrsun--',`on src change -- during loading -- skip`)
            return 
        }
        
        let inActiveId:0| 1 = this.active == 0 ? 1 : 0;
        let newDoc = getSrcDom(newSrc)
        // console.log('tianrsun--',`new Doc`,newDoc)
        this.iframeLoading = true;
        if(inActiveId === 0){
                this.doc0 = newDoc            
        }
        else{
                this.doc1 = newDoc
        }
    }
    iframeOnload(){
        if(this.active == 1){
            this.switchToNewSrc()
        }
    }
    iframeOnload1(){
        if(this.active == 0){
            this.switchToNewSrc()
        }
    }
    /**
     * after inactive iframe been loaded,
     * swap active & inactive iframe
     */
    switchToNewSrc(){
        let newActive: 0 | 1 = this.active == 0 ? 1 : 0;
        // console.log('tianrsun--',`switchToNewSrc new active: ${newActive}`)
        this.active = newActive;
        this.iframeLoading = false;
    }
    get doc(){
        let html = getSrcDom(this.dagSrc)
        return html
    }
}
</script>
<style lang="scss" scoped>
.dag-container{
    height: 100%;
    position: relative;
}
.dag-iframe{
    position: absolute;
    width: 100%;
    height: 100%;
    border: 0;
    visibility: hidden;
    z-index: 100;
    &.active{
        visibility:visible;
        z-index: 1000;
    }
}
</style>

