<template>
  <div
    v-show="visible"
    id="wsReconnect"
    class="el-message el-message--warning"
    role="alert"
  >
    <i class="el-message__icon el-icon-warning" />
    <slot>
      <p class="el-message__content">
        {{ message }}
      </p>
    </slot>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import { wsclient, Status } from '@/net/ws';

@Component({
  components: {}
})
export default class WsReconnect extends Vue {
  wsClient = wsclient;

  get status () {
    return this.wsClient.status;
  }
  get retryTimes () {
    return this.wsClient.reconnectCnt;
  }
  get message() {
    if (this.status === Status.CONNECTING) {
      return 'Connecting to WebSocket server...' + ` retry count: ${this.retryTimes}`;
    } else if (this.status === Status.OFFLINE) {
      return 'Will reconnect to Websocket server...' + ` retry count: ${this.retryTimes}`;
    }
    return '';
  }

  get visible() {
    if (this.status === Status.LOGGED_IN) {
      return false;
    } else {
      return true;
    }
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
#wsReconnect{
    z-index: 1000;
}
</style>


