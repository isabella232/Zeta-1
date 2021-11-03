<template>
  <div class="column-wrapper">
    <template v-if="!isExpanding">
      <div
        v-for="item in showingSets"
        :key="item"
        class="db-text"
      >
        {{ item }}
      </div>
      <button
        v-if="labelSets.length > defaultNumber + 1"
        class="expand"
        @click="() => isExpanding = true"
      >
        +{{ labelSets.length - defaultNumber }}
      </button>
    </template>
    <template v-else>
      <div
        v-for="item in labelSets"
        :key="item"
        class="db-text"
      >
        {{ item }}
      </div>
    </template>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';

@Component({
  components: {},
})
export default class LabelExpansion extends Vue {
  @Prop() labelSets: Array<string>;
  @Prop({
    default: 2,
  }) defaultNumber: number;

  isExpanding = false;

  get showingSets () {
    const {isExpanding, defaultNumber, labelSets} = this;
    if (!isExpanding && labelSets.length - defaultNumber > 1) {
      return labelSets.slice(0, defaultNumber);
    } else {
      return labelSets;
    }
  }

}
</script>
<style lang="scss" scoped>
.column-wrapper {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: left;
  .db-text {
    padding: 1px 8px;
    margin: 4px 8px 4px 0;
    border: 1px solid #a4a7b1;
    border-radius: 2px;
    font-size: 10px;
    background-color: #fafafa;
  }
  .expand {
    width: 37px;
    height: 22px;
    padding: 0, 8px;
    margin-top: 4px;
    background: #ffffff;
    border: 1px dashed #d9d9d9;
    box-sizing: border-box;
    box-shadow: 0px 2px 0px rgba(0, 0, 0, 0.016);
    border-radius: 2px;
    cursor: pointer;
  }
}
</style>
