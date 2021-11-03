import Vue from 'vue';
import { ZetaWebSocketClient } from '@/net/ws';
declare module 'vue/types/vue' {
  interface Vue {
    /** If you do not have a specific DOM node to attach the Loading directive, or if you simply prefer not to use Loading as a directive, you can call this service with some configs to open a Loading instance. */
    $appName: string;
    $breadcrumbs: any;
  }
}
