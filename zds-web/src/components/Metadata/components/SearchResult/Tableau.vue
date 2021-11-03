<template>
  <div>
    <span
      class="content-name"
      @click="jump"
    >{{ data.name }}</span>
    <i class="tableau type-icon" />
    <div style="display: flex; margin: 5px 0;">
      <span style="color: #ff9900; line-height: 20px; margin-right: 5px;">
        {{ scoreStr }}
      </span>
      <el-rate
        :value="score / 2"
        disabled
      />
    </div>
    <div
      v-if="data.desc"
      class="content-desc"
    >
      {{ data.desc }}
    </div>
    <div
      v-if="imgUrl"
      class="img-div"
    >
      <img
        :src="imgUrl"
        @click="jump"
      >
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Inject } from 'vue-property-decorator';
import DoeRemoteService from '@/services/remote/DoeRemoteService';

@Component({})
export default class Tableau extends Vue {
  
  @Inject() apmTransaction: any;
  @Prop() data: any;
  @Prop() pos: any;
  imgUrl = null;
  score = 0;
  get tableauId () {
    return this.data.parsedContent._source.asset_ref_id;
  }
  get scoreStr () {
    return this.score ? this.score.toFixed(1) : '0.0';
  }

  mounted () {
    this.getImg();
    this.getScore();
  }

  async getImg () {
    const param = {
      id: this.data.parsedContent._id,
    };
    this.imgUrl = await DoeRemoteService.instance
      .getTableauImage(param)
      .then(res => {
        return res.data.value;
      });
  }

  async getScore () {
    const param = {
      asset_ref_id: this.tableauId,
      fe_asset_name: this.data.name,
    };
    this.score = await DoeRemoteService.instance
      .getTableauConfidenceScore(param)
      .then((res: any) => {
        if (res && res.data && res.data.data && res.data.data.value) {
          if (res.data.data.value.length > 0) {
            return res.data.data.value[0].score;
          }
        }
      });
  }

  jump () {
    const transaction = this.apmTransaction('metadata', 'view', this.pos);
    setTimeout(() => {
      transaction && transaction.end();
      const url: string = 'https://tableau.corp.ebay.com/#/redirect_to_view/' + this.tableauId;
      window.open(url);
    }, 10);
  }
}
</script>
<style lang="scss">
@import '@/styles/metadata.scss';
.img-div {
  width: 400px;
  height: 226px;

  > img {
    cursor: pointer;
    width: 100%;
    height: 226px;
    background-color: #cacbcf;
    background-repeat: no-repeat;
    box-shadow: 1px 1px 3px rgb(212, 212, 212), -1px -1px 3px rgb(212, 212, 212);
    opacity: 1;
    background-size: cover;
    display: block;
  }
}

</style>