<template>
  <div class="conatiner">
    <navigation />
    <!-- <Breadcrumbs v-if="pathName && pathName != 'Workspace'" /> -->
    <keep-alive>
      <router-view
        v-if="$route.meta.keepAlive"
        class="router-view"
      />
    </keep-alive>
    <router-view
      v-if="!$route.meta.keepAlive && !$route.meta.reload"
      class="router-view"
    />
    <router-view
      v-if="!$route.meta.keepAlive && $route.meta.reload && isRouterAlive"
      class="router-view"
    />
    <exception-list />
    <keep-alive>
      <ZetaAce />
    </keep-alive>
  </div>
</template>


<script lang="ts">
import { Component, Vue, Provide } from 'vue-property-decorator';
import Navigation from './components/navigation/navigation.vue';
// import Breadcrumbs from "./components/common/Breadcrumbs.vue";
import ExceptionList from './components/common/exception-list/exception-list.vue';
import ZetaAce from '@/components/common/zeta-ace/ace.vue';
@Component({
  components: {
    Navigation, ExceptionList, ZetaAce
  },
})

export default class App extends Vue {
  get pathName (){
    return this.$route.name;
  }

  // used for page auto refresh without switching url path
  isRouterAlive = true;
  @Provide('reload')

  reload() {
    this.isRouterAlive = false;
    this.$nextTick(() => this.isRouterAlive = true);
  }
}
</script>
<style lang="scss">
.conatiner{
  // display: flex;
  padding: 0 15px;
  margin-left: 70px;
  height: 100%;
  overflow-x: auto;
}
.breadcrumbs,
.router-view{
  z-index: 9;
  margin: 0 auto;
  min-width: 1000px;
}
.nav{
  z-index: 2001;
}
html,body{
  height: 100%;
  // font-family: 'Helvetica-Bold', 'Helvetica Bold', 'Helvetica';
}
</style>
