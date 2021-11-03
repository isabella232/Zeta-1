<template>
  <div
    class="avatar"
    :style="{height: avatarSize, width: avatarSize}"
    :class="{'avatar-inline': inline, 'avatar-cycle': cycle}"
  >
    <img
      class="avatar-img"
      :style="{height: avatarSize, width: avatarSize}"
      :src="avatarUrl()"
      @error="setAltImg"
    >
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import config from '@/config/config';
@Component({
  components: {}
})
export default class DssAvatar extends Vue {
  @Prop()
  nt: string;

  @Prop({ default: false, type: Boolean })
  cycle: boolean;

  @Prop({ default: false, type: Boolean })
  inline: boolean;

  @Prop({ default: 'default'})
  size: string;

  readonly defaultAvatar = './img/icons/user_gray.png';

  readonly defaultSize = '30px';
  readonly smallize = '24px';
  readonly sizeMap = {
    'default': '30px',
    'small': '24px',
  };

  get avatarSize () {
    let fontSize = this.sizeMap.default;
    if (this.size) {
      if (this.sizeMap[this.size]) {
        fontSize = this.sizeMap[this.size];
      } else {
        try {
          const s = parseInt(this.size);
          fontSize = s + 'px';
        } catch {

        }
      }
    }
    return fontSize;
  }
  avatarUrl () {
    return `${config.oss.avatarUrl}${this.nt}`;
  }

  setAltImg (event) {
    event.target.src = this.defaultAvatar;
  }
}
</script>
<style lang="scss" scoped>
.avatar {
  border-radius: 15px;
}
.avatar-inline {
  display: inline-block;
  vertical-align: middle;
}
.avatar-cycle {
  border-radius: 50%;
}
</style>


