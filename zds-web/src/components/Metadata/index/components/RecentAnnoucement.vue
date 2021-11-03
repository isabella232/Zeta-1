<template>
  <div class="recent-announcement">
    <h1>Recent Announcements</h1>
    <div
      v-for="(i, $index) in announcements"
      :key="$index"
      class="item"
    >
      <div class="index">
        {{ getIndex($index + 1) }}
      </div>
      <div style="width:90%">
        <a
          class="title"
          :href="i.link"
          :title="i.enotify_title"
          target="__blank"
        >
          {{ i.enotify_title }}
        </a>
        <footer class="title">
          {{ i.create_time }}
        </footer>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { padStart } from 'lodash';
import { Component, Vue } from 'vue-property-decorator';
import { Action, State } from 'vuex-class';
import { Actions } from '../store';

@Component({})
export default class RecentAnnoucement extends Vue {
  @Action(Actions.GetRecentAnnouncement) getAnnouncements;
  @State(state => state.MetadataIndex.recentAnnouncements) announcements;

  created () {
    this.getAnnouncements();
  }

  getIndex (i) {
    return padStart(i, 2, '0');
  }

}
</script>
<style lang="scss" scoped>

.recent-announcement {
  padding-top: 42px;
  h1 {
    font-size: 18px;
  }
  .item {
    display: flex;
    padding: 8px 0;
    .index {
      color: #BDBDBD;
      font-size: 24px;
      padding: 4px 8px 4px 0;
    }
    a {
      color: #4F4F4F;
      text-decoration: none;
      font-size: 16px;
      padding-top: 7px;
      text-overflow: ellipsis;
    }
    footer {
      color: #BDBDBD;
    }

  }

}

</style>