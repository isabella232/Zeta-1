<template>
  <div class="settings-container">
    <el-tabs
      v-model="activeTab"
      tab-position="left"
    >
      <el-tab-pane
        v-for="c in tabComponents"
        :key="c"
        :label="c"
        :name="c"
      >
        <component :is="c" />
      </el-tab-pane>
      <!-- <el-tab-pane label="Profile">
        <Profile />
      </el-tab-pane>
      <el-tab-pane label="Preference">
        <UserPreference />
      </el-tab-pane>
      <el-tab-pane label="Hermes">
        <HermesPreference />
      </el-tab-pane> -->
    </el-tabs>
  </div>
</template>
<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import Profile from '@/components/Settings/Profile.vue';
import UserPreference from '@/components/Settings/user-preference.vue';
import HermesPreference from '@/components/Settings/hermes-preference.vue';
import { Watch } from 'vue-property-decorator';
@Component({
  components: {Profile, UserPreference, HermesPreference},
})
export default class Settings extends Vue {
  tabComponents = ['Profile', 'UserPreference', 'HermesPreference'];
  activeTab = 'Profile';

  @Watch('$route.query.preference', { immediate: true })
  onTabchange (newVal: string) {
    if (newVal && this.tabComponents.indexOf(newVal) >= 0) {
      this.activeTab = newVal;
    }
  }
}
</script>
<style lang="scss" scoped>
.settings-container {
  width: 700px;
}
</style>
