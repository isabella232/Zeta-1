<template>
  <div tabIndex="0" @blur='open=false;' class="pvtDropdown">
    <div @click='toggle()' :class='["pvtDropdownValue", "pvtDropdownCurrent", {"pvtDropdownCurrentOpen": open}]' role="button"> 
      <div class="pvtDropdownIcon">{{open ? '×' : '▾'}}</div>
      <!--
      {{current}} 
      -->
      <span>{{value}}</span>
    </div>
    <div v-if="options && open" class='pvtDropdownMenu'>
      <div v-for="v in options" 
        :key="v" role='button' 
        :class='["pvtDropdownList", "pvtDropdownValue",{"pvtDropdownActiveValue": isActiveValue(v)}]'
        @click='clickDropdownValue(v)'>
        <span>{{v}}</span>
      </div>
    </div>
  </div>
  
</template>
<script lang='ts'>
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class Dropdown extends Vue {
  @Prop() value!: string;
  @Prop() options!: string[];

  open: boolean = false;

  constructor() {
    super();
  }

  isActiveValue(v: string) {
    return this.value === v;
  }

  clickDropdownValue(value: string) {
    if ( this.value === value ) {
      this.toggle();
      return;
    }
    this.setCurrent(value);
    this.toggle();
  }

  setCurrent(value: string) {
    this.$emit('input', value);
  }

  /*
  beforeMount() {
  }
  */

  toggle() {
    this.open = !this.open;
  }
}
</script>
<style scoped lang='scss'>
.pvtDropdown {
    display: inline-block;
    position: relative;
    user-select: none;
    // margin: 3px;
}
.pvtDropdown:focus {
  outline: none;
}
.pvtDropdownValue {
    padding: 2px 5px;
    //font-size: 12px;
    text-align: left;
    font-weight: 100;
}
.pvtDropdownCurrent {
    text-align: left;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    display: inline-block;
    position: relative;
    width: 100%;
    box-sizing: border-box;
    background: #fff;
}
.pvtDropdownCurrentOpen {
    border-radius: 4px 4px 0 0;
}
.pvtDropdownIcon {
    float: right;
    color: #dcdfe6;
}
.pvtDropdownMenu {
    background: #fff;
    position: absolute;
    width: 100%;
    max-height: 150px;
    overflow-y: auto;
    margin-top: -1px;
    border-radius: 0 0 4px 4px;
    border: 1px solid #dcdfe6;
    border-top: 1px solid #dfe8f3;
    box-sizing: border-box;
    z-index: 100;
}
.pvtDropdownActiveValue {
    background: #ebf0f8;
}
.pvtDropdownList:hover {
    background: #e0f0f8;
}
[role=button] {
    cursor: pointer;
}
</style>
