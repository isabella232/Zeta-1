<template>
  <el-radio-group
    :value="value"
    @input="setPlatform"
  >
    <el-radio
      v-for="(name, platform) in option.platform"
      :key="platform"
      :label="platform"
      :disabled="disableByPermission(platform)"
    >
      {{ name }}
    </el-radio>
  </el-radio-group>
</template>

<script lang="ts">
import _ from 'lodash';
import { Component, Vue, Prop, Watch, Emit, Inject } from 'vue-property-decorator';
import { NoteOption } from './note-options';
@Component({
  components: {}
})
export default class NotebookPlatformSelector extends Vue {
  @Inject()
  isDisabledPlatform: (platform: string) => boolean;

  @Prop({})
  option: NoteOption;

  @Prop()
  value: string;

  // get disableSelect() {
  //   let platformCnt = 0;
  //   if (this.option && this.option.platform) {
  //     platformCnt = _.keys(this.option.platform).length;
  //   }
  //   return Boolean(platformCnt <= 1) || this.disableByPermission;
  // }

  disableByPermission(plat: string) {
    return this.isDisabledPlatform(plat);
    // const platform = this.value;
    // let access = true;
    // if (_.has(this.platformAccess, platform)) {
    //   access = this.platformAccess[platform];
    // }
    // return access;
  }
  @Watch('option', { immediate: true })
  setDefaultValue(option: NoteOption) {
    if (!option || _.isEmpty(option.platform)) {
      return;
    }
    const platforms = _.keys(option.platform);
    this.setPlatform(platforms[0]);
  }

  @Emit('input')
  // eslint-disable-next-line @typescript-eslint/no-empty-function, @typescript-eslint/no-unused-vars
  setPlatform(platform: string) {
    return platform;
  }


}
</script>

<style lang="scss" scoped>

</style>
