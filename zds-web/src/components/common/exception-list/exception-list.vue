<template>
<div class="zeta-error-message" >
    <ExceptionItem v-for="e in exceptions" :key="e.id" :exception="e"/>
</div>  
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Emit } from 'vue-property-decorator';
import _ from 'lodash';
import { ZetaException } from '@/types/exception';
import { IWorkspace } from '@/types/workspace';
import ExceptionItem from './exception-item.vue';

@Component({
  components: {
      ExceptionItem
  }
})
export default class ExceptionList extends Vue { 

    get exceptions(): ZetaException[] {
        return _.chain(this.$store.getters.zetaExceptions)
        .filter((e: ZetaException) => {
            return e.resolved == false
        })
        .sort((e1,e2) => {
            return e2.createTime - e1.createTime
        } ).value();
    }


    resolve(e: ZetaException) {
        e.resolve()
    }
    
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
.zeta-icon-info {
    color: red;
}
.zeta-error-message {
    position: fixed;
    top: 120px;
    right: 20px;
    z-index: 100000;
}
</style>
