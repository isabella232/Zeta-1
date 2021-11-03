<template>
  <div class="domain-card">
    <header>
      <img :src="icon">
      <a @click.stop="actions[0].action">{{ title }}</a>
    </header>
    <div class="metrics">
      <div class="metric">
        <div>Total Tables</div>
        <div>{{ tables || 0 }}</div>
      </div>
      <div class="metric">
        <div>Unique Users</div>
        <div>{{ users || 0 }}</div>
      </div>
      <div class="metric">
        <div>Unique Batches</div>
        <div>{{ batches || 0 }}</div>
      </div>
    </div>
    <footer>
      <a
        v-for="a in stagedActions"
        :key="a.label"
        @click.stop="a.action"
      >{{ a.label }}</a>
      <el-dropdown v-if="hideActions.length > 0">
        <span class="more">More</span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item
            v-for="a in hideActions"
            :key="a.label"
          >
            <div @click.stop="a.action">
              {{ a.label }}
            </div>
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </footer>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';

@Component({
  components: {
  },
})
export default class DomainCard extends Vue {

  @Prop() title: string;
  @Prop() tables: number;
  @Prop() users: number;
  @Prop() batches: number;
  @Prop() actions: { label: string; action: Function }[];

  get stagedActions () {
    return this.actions.slice(0, 4);
  }
  get hideActions () {
    return this.actions.slice(4);
  }
  get icon () {
    return require(`./icon/${this.title}.svg`);
  }
}
</script>
<style lang="scss" scoped>
.domain-card {
  box-shadow: 0px 0px 5px rgba(0, 0, 0, 0.15);
  position: relative;
  &:hover {
    box-shadow: 0px 9px 15px rgba(0, 0, 0, 0.15);
    footer {
      background: darken(#f8f8f8, 5%);
    }
  }
  > header {
    padding: 12px 18px;
    display: flex;
    align-items: center;
    > img {
      display: inline-block;
      width: 24px;
      height: 24px;
    }
    > a {
      font-size: 16px;
      font-weight: bold;
      padding-left: 8px;
      color: #333;
      cursor: pointer;
    }
  }
  > .metrics {
    padding: 24px 0;
    display: flex;
    justify-content: space-between;
    > .metric {
      text-align: center;
      width: 100%;
      > div:first-of-type {
        font-size: 16px;
        color: #bdbdbd;
        font-weight: bold;
      }
      > div:last-of-type {
        font-size: 20px;
        color: #4F4F4F;
        font-weight: bold;
      }
    }
  }
  footer {
    padding: 12px 0;
    background: #f8f8f8;
    text-align: center;
    span.more,
    > a {
      color: #333;
      font-weight: bold;
      cursor: pointer;
      font-size: 1em;;
      &:hover {
        color: #164D97;
      }
      &:not(:last-of-type)::after {
        content: '';
        display: inline-block;
        height: 1.2em;
        width: 1px;
        background: lighten(#333, 40%);
        margin: 0 8px;
        vertical-align: sub;
      }
    }
    span.more::before {
      content: '';
      display: inline-block;
      height: 1.2em;
      width: 1px;
      background: lighten(#333, 40%);
      margin: 0 8px;
      vertical-align: sub;
    }
  }
}
</style>