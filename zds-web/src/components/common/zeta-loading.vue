<template>
  <div
    v-show="display"
    class="zeta-loading"
  >
    <div class="zeta-loading-spinner">
      <span class="zeta-loading-context">
        {{ percentage }}%
      </span>
      
      <el-progress
        style="maigin-top: 10px;"
        :show-text="false"
        :percentage="percentage"
      />
      <span class="zeta-loading-context">
        {{ message }}
      </span>
    </div>
  </div>  
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

@Component({
  components: {
  }
})
export default class ZetaLoading extends Vue { 
  display = false;
  percentage = 0;
  message = '';
  loading(message = '', pctg?: number) {
    this.display = true;
    if (message) {
      this.message = message;
    }
    if (pctg) {
      this.percentage = pctg;
    }
  }
  close() {
    this.display = false;
    
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';
$color: #94BFE1;
.zeta-loading{
  position: absolute;
  z-index: 2000;
  background-color: #E5E5E5;
  margin: 0;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  transition: opacity .3s;
  display: flex;
  justify-content: center;
  .zeta-loading-spinner {
    top: 50%;
    margin-top: -21px;
    width: 60%;
    text-align: center;
    position: absolute;
    color: #409eff;
    margin: 3px 0;
    font-size: 14px;
    .zeta-loading-context {
      margin-bottom: 10px;
      font-size: 24px;
      color: $color;
    }
    /deep/ .el-progress-bar__inner {
      background: linear-gradient(90deg, #67D1EC 0%, #468AE6 100.32%);;
    }
  }
}
</style>
