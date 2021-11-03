<template>
  <div class="card">
    <div class="card-header">
      <div class="name">
        {{ name }}
      </div>
      <div class="quick-route">
        <slot name="quick-route" />
      </div>
      <div class="image">
        <img :src="imgSrc">
      </div>
    </div>
    <div class="content">
      <slot />
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
@Component
export default class CardTemplate extends Vue {
  @Prop()
  name: string;
  @Prop()
  imgSrc: string;
}
</script>
<style lang="scss" scoped>
$height: 236px;
$headerHeight: 83px;
.card {
  margin: 18px;
  height: $height;
  width: 560px;
  background-color: #FFF;
  box-sizing: border-box;
  overflow: hidden;
  filter: drop-shadow(0px 0px 8px rgba(0, 0, 0, 0.15));
  border: 1px solid #F5F7FA;
  &:hover {
    border: 1px solid #94BFE1;
  }
  .card-header {
    height: $headerHeight - 20px;
    background: linear-gradient(0deg, #F5F7FA, #F5F7FA), #F8FAFF;
    position: relative;
    overflow: hidden;
    padding-top: 20px;
    padding-left: 20px;
    .name {
      margin-bottom: 9px;
      font-family: Arial;
      font-style: normal;
      font-weight: bold;
      font-size: 16px;
      line-height: 18px;
    }
    .image {
      position: absolute;
      height: 104px;
      width: 104px;
      top: -9px;
      left: 490px;
      opacity: .5;
      img {
        width: 100%;
        height: 100%;
      }
    }
    .splitter {
      height: 100%;
      width: 1px;
      margin: 0 5px;
      border-left: 1px solid rgba(0, 0, 0, 0.2);
    }
  }
  .content {
    height: $height - $headerHeight - 20px - 40px;
    padding: 20px 20px;
    .intro-container {
      position: relative;
      .image {
        z-index: -1;
        position: absolute;
        height: 149px;
        width: 149px;
        left: -43px;
        top: -5px;
        opacity: .5;
        img {
          width: 100%;
          height: 100%;
        }
      }
      .intro {
        margin-top: 30px;
        font-family: Arial;
        font-style: normal;
        font-weight: bold;
        font-size: 12px;
        line-height: 16px;
        display: flex;
        align-items: center;
      }
    }
  }
}
</style>
