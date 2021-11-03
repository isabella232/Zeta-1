<template>
  <div class="faq-container">
    <FAQList />
  </div>
</template>
<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import  FAQList from '@/components/FAQ/FAQList.vue';
@Component({
  components: {
    FAQList
  },
})
export default class FAQ extends Vue {}
</script>