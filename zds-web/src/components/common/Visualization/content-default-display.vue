<template>
  <div class="content-default-display">
    <div
      v-for="(content,$i) in contents"
      :key="$i"
    >
      <template v-if="content.type === 'TEXT'">
        <div class="content-text-display">
          {{ content.content }}
        </div>
      </template>
      <template v-if="content.type === 'HTML'">
        <div
          class="content-html-display"
          v-html="content.content"
        />
      </template>
      <template v-if="content.type === 'IMG'">
        <div class="content-img-display">
          <img :src="'data:image/png;base64,'+ content.content">
        </div>
      </template>
    </div>
  </div>
</template>

<script lang="ts">

import { Component, Vue, Prop, Watch } from 'vue-property-decorator';
import _ from 'lodash';
import { QueryContent } from '@/types/workspace/notebook-job';

@Component({
  components: {  }
})

export default class ContentDefaultDisplay extends Vue {
  @Prop()
  contents: QueryContent;
}
</script>

<style lang="scss" scoped>
.content-default-display {
	padding: 20px;
	.content-text-display {
		white-space: pre-wrap;
		font-family: monospace;
	}
}

</style>
