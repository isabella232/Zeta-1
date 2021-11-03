<template>
  <span
    v-if="nt"
    class="slack-icon" 
    @click="invokeSlackApp"
  >
    <slot />
  </span>
</template>

<script lang="ts">
import { Vue, Component, Prop } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';
const api = DoeRemoteService.instance;

@Component({})
export default class SlackInvoker extends Vue {
  @Prop() nt: string;

  async invokeSlackApp () {
    const slackUserId = await api.getSlackUserId(this.nt);
    window.open(`slack://user?team=T0M05TDH6&id=${slackUserId}`);
  }
}
</script>

<style lang="scss" scoped>

</style>
