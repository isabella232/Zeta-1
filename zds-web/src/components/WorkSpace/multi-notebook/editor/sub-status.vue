<template>
<div class="sub-editor-status">
    <span class="status-dot" :class="className"> </span>
</div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Inject, Emit } from 'vue-property-decorator'

@Component({
  components: {
  },
})
export default class SubEditorStatus extends Vue {
   @Prop({ default: 'OFFLINE'})
   status: string

   get className() {
       const status = this.status.toLowerCase()
       let className = `status-dot-${status}`
       return className;
   }
}


</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
$pending: #f6ba68;
$connect: #5cb85c;
$offline: #ddd;
$running: rgb(86, 156, 226);
@keyframes fade {
    from {
        background-color: transparent;
    }
    50% {
        background-color: #5cb85c;
    }
    to {
        background-color: transparent;
    }
}
.status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: inline-block;
    &.status-dot-connecting {
        animation: fade 1000ms infinite;
        background-color: $connect;
    }
    &.status-dot-idle {
        background-color: $connect;
    }
    &.status-dot-offline {
        background-color: $offline;
    }
    &.status-dot-running {
        background-color: $running;
    }
    &.status-dot-pending {
        background-color: $pending;
    }
}
</style>