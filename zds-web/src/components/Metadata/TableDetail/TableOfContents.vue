<template>
  <div class="table-of-contents">
    <div
      v-for="n in contents"
      :key="n.id"
      :class="actived === n.id && 'actived'"
    >
      <h1
        class="label"
        @click="() => onClick(n.id)"
      >
        {{ n.label }}
      </h1>

      <div
        v-if="n.children"
        class="sub-items"
      >
        <h2
          v-for="c in n.children"
          :key="c.id"
          :class="actived === c.id && 'actived'"
          @click="() => onClick(c.id, true)"
        >
          {{ c.label }}
        </h2>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';

interface Node {
  id: string;
  label: string;
}

interface ParentNode extends Node {
  children?: Node[];
}

export type Contents = ParentNode[];

@Component({
  components: {
  },
})
export default class TableOfContents extends Vue {

  @Prop() contents: ParentNode;
  @Prop() actived: string;

  back () {
    this.$router.back();
  }

  onClick (id, isSub = false) {
    this.$emit('select', id, isSub);
  }
}
</script>
<style lang="scss" scoped>

$primary: #659CDC;

.table-of-contents {
  h1.label {
    font-size: 18px;
    cursor: pointer;
  }
  .actived h1 {
    color: $primary;
  }

  .sub-items {
    border-left: 1px solid #ccc;
    margin: 6px;
    color: #585E65;
    font-size: 16px;
    padding-left: 18px;
    h2 {
      font-size: 16px;
      font-weight: normal;
      cursor: pointer;
    }
    h2:not(:first-of-type) {
      padding-top: 6px;
    }
    h2.actived {
      color: $primary;
    }
  }
}

</style>