<template>
  <div>
    <span
      class="content-name"
      @click="jump"
    >{{ data.name }}</span>
    <i class="udf type-icon" />

    <div
      v-if="data.desc"
      class="content-desc"
    >
      {{ data.desc }}
    </div>
    <div
      v-if="data.example"
      class="content-desc"
    >
      {{ item.example }}
    </div>
    <div>
      <i
        v-for="(p, $j) in platforms"
        :key="$j"
        :title="p"
        :class="p.toLowerCase()"
        class="platform-icon"
      />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import { attempt } from '@drewxiu/utils';

@Component({})
export default class Udf extends Vue {
  
  @Inject() apmTransaction: any;
  @Prop() data: any;
  @Prop() pos: any;

  get platforms () {
    return attempt(() => 
      this.data.parsedContent._source.platform.split(',')
    , []);
  }

  jump () {
    const { parsedContent, name } = this.data;
    this.apmTransaction('metadata', 'view', this.pos);
    this.$router.push(`/metadata/udf/${parsedContent._source.db_name}.${name}`);
  }

}
</script>
<style lang="scss">
@import '@/styles/metadata.scss';
</style>