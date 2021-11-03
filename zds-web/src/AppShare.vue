<template>
  <div class="conatiner">
    <navigation />
    <Breadcrumbs />
    <router-view class="router-view" />
  </div>
</template>


<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import Navigation from './components/navigation/navigation.vue';
import Breadcrumbs from './components/common/Breadcrumbs.vue';
@Component({
  components: {
    Navigation, Breadcrumbs
  },
})

export default class App extends Vue {
}
</script>
<style lang="scss">
.conatiner{
  // display: flex;
  padding: 0 15px;
  margin-left: 70px;
  height: 100%;
}
.router-view{
  z-index: 9;
  margin: 0 auto;
  height: calc( 100% - 60px);
  /deep/ .tools-container{
    height: calc(100% - 10px);
  }
}
.nav{
  z-index: 2001;
}
html,body{
  height: 100%;
  // font-family: 'Helvetica-Bold', 'Helvetica Bold', 'Helvetica';
}
</style>