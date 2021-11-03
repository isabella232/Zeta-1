<template>
  <div class="workspace-guide">
    <HomePage />
  </div>
</template>


<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import HomePage from '@/views/home-page-view.vue';

@Component({
  components: {
    HomePage,
  },
})
export default class WorkspaceGuide extends Vue {
}
</script>
