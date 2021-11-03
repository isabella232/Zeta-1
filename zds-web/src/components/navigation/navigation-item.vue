<template>
  <div
    class="nav-item"
    @click="onClick"
  >
    <div class="nav-item-icon">
      <template v-if="isImage">
        <dss-avatar
          :nt="nt"
          cycle
          class="header-img"
        />
      </template>
      <i
        v-else
        :class="icon"
      />
    </div>

    <div class="nav-item-name">
      {{ name }}
    </div>
  </div>
</template>

<script lang="ts">
import Util from '@/services/Util.service';
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';

@Component({
  components: {
  },
})
export default class NavigationItem extends Vue {
  @Prop()
  name: string;

  @Prop()
  icon: string;

  @Prop()
  img: string;

  @Prop()
  path: string;

  @Prop()
  url: string;

  @Inject()
  pathTo: (path: string) => void;

  get isImage() {
    return  Boolean(this.img);
  }
  get nt () {
    return Util.getNt();
  }
  onClick() {
    const listener = this.$listeners.click;
    if (listener && typeof listener === 'function') {
      listener();
      return;
    }
    if (this.path) {
      this.pathTo(this.path);
      return;
    } else if (this.url) {
      window.open(this.url, this.name);
      return;
    }

  }

}
</script>
