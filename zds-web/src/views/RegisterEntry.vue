<template>
  <div class="register-container">
    <el-breadcrumb separator-class="el-icon-arrow-right">
      <el-breadcrumb-item>
        <span @click="backToMain">Metadata</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item>Register</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="content">
      <h3>Choose your metadata entity</h3>
      <el-select
        v-model="type"
        size="large"
      >
        <el-option
          v-for="item in metadataTypes"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-button
        type="primary"
        :disabled="!type"
        size="large"
        @click="next"
      >
        Next
      </el-button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import MetadataRegisterVDM from '@/components/WorkSpace/Metadata/MetadataRegisterVDM.vue';

@Component({
  components: {
    MetadataRegisterVDM
  }
})
export default class RegisterVDM extends Vue {

  metadataTypes = [
    {
      value: 'vdm',
      label: 'VDM',
    },
    {
      value: 'udf',
      label: 'User-defined Function (UDF)',
    }
  ];

  type: 'vdm' | 'udf' = 'vdm';

  next() {
    let path = '';
    switch (this.type) {
      case 'vdm':
        path = '/metadata/register/vdm';
        break;
      case 'udf':
        path = '/metadata/register/udf';
        break;
    }
    this.$router.push(path);
  }

  backToMain() {
    this.$router.push('/metadata');
  }

}
</script>
<style lang="scss" scoped>
@import '@/styles/global.scss';
.register-container{
  padding: 18px;

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

  .content {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 80vh;
    flex-direction: column;
    > * {
      margin-bottom: 24px;
      width: 300px;

      /deep/ input {
        height: 40px;
      }
    }
  }
}
</style>
