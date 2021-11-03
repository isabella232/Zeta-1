<template>
  <div class="header">
    <div class="left">
      <div style="display:inline-block;">
        <div 
          v-if="showBackButton && breadcrumb.length" 
          class="icon-back"
        >
          <a @click="backAction ? backAction() : back()">
            <i class="el-icon-back" />
          </a>
        </div>
        <div
          v-if="breadcrumb.length"
          class="breadcrumb"
        >
          <el-breadcrumb separator-class="el-icon-arrow-right"> 
            <el-breadcrumb-item
              v-for="item in clickBreadCrumbs"
              :key="item.key"
              class="breadcrumb-router"
            >
              <span @click="routerTo(item.key)">{{ item.label }} </span>
            </el-breadcrumb-item>
            <el-breadcrumb-item>
              <span> {{ lastBreadCrumbItem[0].label }} </span>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
      </div>
      <search-box style="position:relative; top:-16px; left:28px;" />
    </div>
    <div class="right">
      <div class="menu">
        <el-dropdown
          trigger="click"
          @command="(cmd) => {this.$router.push(cmd)}"
        >
          <i
            class="el-icon-menu"
            :class="{ dropped: isDroppingDown === true }"
            @click="toggleDroppingStatus"
            @blur="() => isDroppingDown = false"
          />
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="/metadata/sa">
              <i class="el-icon-notebook-1" />
              Subject Area
            </el-dropdown-item>
            <el-dropdown-item command="/tablemanagement">
              <i class="el-icon-tickets" />
              Table Management
            </el-dropdown-item>
            <el-dropdown-item command="/metadata/register/vdm">
              <i class="el-icon-folder-add" />
              VDM Register
            </el-dropdown-item>
            <el-dropdown-item command="/metadata/register/udf">
              <i class="el-icon-folder-add" />
              UDF Register
            </el-dropdown-item>
            <el-dropdown-item command="/metadata/collection">
              <i class="el-icon-collection-tag" />
              My Collection
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import SearchBox from './SearchBox.vue';

@Component({
  components: {
    SearchBox,
  },
})
export default class Header extends Vue {
  @Prop() breadcrumb: Array<object>; 
  @Prop() showBackButton: boolean;
  @Prop() backAction: Function;

  isDroppingDown = false;

  get clickBreadCrumbs () {
    if (!this.breadcrumb) return [];
    return this.breadcrumb.slice(0, -1);
  }

  get lastBreadCrumbItem () {
    if (!this.breadcrumb) return [];
    return this.breadcrumb.slice(-1);
  }

  toggleDroppingStatus () {
    this.isDroppingDown = !this.isDroppingDown;
  }

  back () {
    this.$router.back();
  }

  routerTo (route) {
    this.$emit('breadcrumb-click', route);
  }
  
}
</script>
<style lang="scss" scoped>
$gray: #cacbcf;
*:focus {
  outline: none;
}
.header {
  display: flex;
  justify-content: space-between;
  height: 56px;
  margin-top: 0;
  border-bottom: 2px solid $gray;
  box-sizing: border-box;
  position: sticky;
  top: 0;
  background: white;
  z-index: 9;
  .left {
    display: flex;
    padding-top: 16px;
    h1 {
      display: inline-block;
      font-size: 18px;
    }
    .icon-back {
      display: inline-block;
      position: relative;
      bottom: 2px;
      cursor: pointer;
      &::after {
        content: "";
        display: inline-block;
        height: 16px;
        width: 1px;
        background: #cacbcf;
        margin: 0 4px;
        vertical-align: text-bottom;
      }
    }
    .breadcrumb {
      display: inline-block;
      .el-breadcrumb__item {
        &:not(:last-of-type) {
        cursor: pointer;
        span {
          color: #4d8cca;
          &:hover {
            text-decoration: underline;
            }
          }
        }
      }
    }
  }
  .right {
    display: inline-block;
    .menu {
      display: inline-block;
      height: 56px;
      overflow: hidden;
      .el-icon-menu {
        padding: 18px 20px 0;
        cursor: pointer;
      }
      ::v-deep .dropped {
        &::before {
          color: #659cdc;
        }
      }
    }
  }
}
</style>
