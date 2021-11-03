<template>
  <div class="notify-container">
    <div v-if="isEnotify(notify)" class="notify-item">
      <p class="notify-type">
        <i 
          class="el-icon-message-solid"
          :class="notify.body.type" 
        />eNotify
      </p>
      <p class="type">
        <em>Type:</em>{{ notify.body.type }}
      </p>
      <p class="product">
        <em>Product:</em>{{ notify.body.product }}
      </p>
      <p class="date">
        <em>Date:</em>{{ formatTime(notify.body.createTime) }}
      </p>
      <p v-if="notify.body.startTime" class="date">
        <em>StartTime:</em>{{ formatTime(notify.body.startTime) }}
      </p>
      <p v-if="notify.body.endTime" class="date">
        <em>EndTime:</em>{{ formatTime(notify.body.endTime) }}
      </p>
      <p class="detail">
        <em>Detail:</em>
        <el-popover 
          placement="top-start" 
          trigger="click"
          width="700"
        >
          <iframe 
            :src="notify.body.link" 
            style="width: 100%; height: 600px; border: 0;" 
          />
          <div slot="reference">
            <span>{{ notify.body.title }}</span>
          </div>
        </el-popover>
      </p>
    </div>
    <div v-else class="notify-item">
      <p class="notify-type">
        <i 
          class="el-icon-message-solid zeta-notify"
        />Notification
      </p>
      <!-- <p class="title">
        <em>Title:</em>{{ notify.body.content }}
      </p> -->
      <p class="detail notify-content">
        <em>Detail:</em><span v-html="notify.body.content" />
      </p>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import moment from 'moment';
import { Notification, NotifyType } from '@/types/ace/index';

@Component({
  components: {

  },
})
export default class NotifyView extends Vue {
  @Prop() notify: Notification;

  isEnotify(notify: Notification){
    return notify.type === NotifyType.ENOTIFY;
  }
  formatTime(time: string){
    return moment(time).local().format('YYYY-MM-DD HH:mm:ss');
  }
}
</script>
<style lang="scss" scoped>
@import "@/styles/global.scss";
.notify-item{
    .notify-type{
        line-height: 20px;
        font-size: 14px;
        i{
            font-size: 16px;
            margin-right: 2px;
        }
        .MAINTENANCE{
            color: $zeta-global-color-brown
        }
        .GENERAL{
            color: $zeta-global-color-green
        }
        .PRODUCT{
            color: $zeta-global-color
        }
        .zeta-notify{
            color: $zeta-global-color
        }
    }
    .type,
    .date,
    .product,
    .detail,
    .title{
      color: #333;
      display: grid;
      grid-template-columns: auto auto;
      justify-content: start;
      >em{
          font-style: normal;
          font-weight: normal !important;
          margin-right: 5px;
          color: $zeta-font-light-color;
      }
      >span{
          color: $zeta-global-color;
          cursor: pointer;
          &:hover{
            text-decoration: underline;
          }
      }
    }
    .notify-content{
      >span{
        color: #333;
        cursor: default;
        &:hover{
          text-decoration: none;
        }
      }
    }
}
</style>
