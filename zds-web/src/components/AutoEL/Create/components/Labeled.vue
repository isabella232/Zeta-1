<template>
  <div class="labeled">
    <slot name="label">
      <label :style="{ width: width, textAlign: labelAlign }">{{ label }}</label>
    </slot>
    <div class="slot">
      <slot />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';

@Component({
  components: {
  },
})
export default class Labeled extends Vue {
  @Prop() label;
  @Prop() width;
  @Prop({
    default: () => 'left',
  }) labelAlign;

}
</script>

<style lang="scss" scoped>
.labeled {
  padding: 8px 0;
  display: flex;
  label {
    width: 140px;
    flex-shrink: 0;
    flex-grow: 0;
    line-height: 30px;
    color: #333;
  }
  .slot {
    line-height: 30px;
    width: 100%;
    color: #999;
  }
}
</style>