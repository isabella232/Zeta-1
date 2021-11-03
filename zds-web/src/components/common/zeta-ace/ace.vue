<template>
  <div class="zeta-ace-wrapper" :class="[visible?'show':'']">
    <div class="ace-button" @click="changeVisible" @mousedown.stop="move">
      <i class="zeta-icon-help" />
      <el-badge
        :value="filterNotification.length"
        :max="20"
        :hidden="filterNotification.length===0?true:false"
        class="item zeta-enotify"
      />
    </div>
    <div ref="ace" class="zeta-ace-main" :class="className" :style="style">
      <div class="ace-body" :style="bodyStyle">
        <div class="header">
          <div class="layout-ctrl" v-click-metric:ACE_CLICK="{name: 'fullScreen'}">
            <i class="icon zeta-icon-fullScreen" v-if="fullScreen" @click="handleFullscreen(1)"/>
            <i class="icon zeta-icon-fullScreen-exit" v-else @click="handleFullscreen(0)"/>
          </div>
          <span>Zeta Ace</span>
          <i class="el-icon-minus" @click="setVisible(false)" />
        </div>
        <div class="content" ref="content">
          <div class="message-list defaultQA">
            <div class="message-box">
              <p class="day">{{day}}</p>
              <div class="message-content">
                <div class="header-img">Z</div>
                <div class="message-data">
                  <p class="title">Hi, I’m Zeta Ace, what’s new?</p>
                  <div class="detail">
                    <ul>
                      <li
                        @click="sendDefaultQ(item)"
                        v-for="(item,i) in aceDefaultQuestion"
                        :key="i"
                        v-click-metric:ACE_CLICK="{name: 'clickDefaultQuestion'}"
                      >Q{{i+1}}. {{item.title}}</li>
                    </ul>
                  </div>
                </div>
              </div>
              <p class="time">{{time}}</p>
            </div>
          </div>
          <div
            class="message-list"
            v-for="(item,$i) in messageList"
            :key="$i"
            :class="item.user == aceNt?'ace':'customer'"
          >
            <div class="message-box">
              <p class="day">{{item.day}}</p>
              <div class="message-content">
                <dss-avatar
                  v-if="item.user!=='ace'"
                  :nt="item.user"
                  class="header-img"
                />
                <div class="header-img" v-else>Z</div>
                <div class="message-data">
                  <p class="title" v-if="item.title">{{item.title}}</p>
                  <p class="question" v-if="item.question" v-html="item.question"></p>
                  <p class="space" v-if="(item.question||item.title)&&item.detail"></p>
                  <div class="detail" v-if="item.detail">
                    <ul v-if="item.detail.list">
                      <li v-for="dq in item.detail.list" :key="dq.question">
                        <a :href="dq.href" v-if="dq.href&&dq.title">{{dq.title}}</a>
                        <p v-if="dq.question">{{dq.question}}</p>
                        <p v-if="dq.description">{{dq.description}}</p>
                      </li>
                    </ul>
                    <div v-if="item.type === 'ACE_ENOTIFY' " class="notify">
                      <NotifyView :notify="item.detail.notify" />
                    </div>
                    <div v-if="item.type === 'ACE_QUESTION' " class="faq">
                      <div class="faq-question">
                        <em>Question:</em>
                        <p>{{item.detail.notify.question}}</p>
                      </div>
                      <div class="faq-answer">
                        <em>Answer:</em>
                        <p v-html="item.detail.notify.answer"></p>
                      </div>
                    </div>
                    <div v-if="item.detail.faq" class="faq">
                      <div class="faq-question">
                        <em>Question:</em>
                        <p>{{item.detail.faq.question.title}}</p>
                      </div>
                      <div class="faq-answer">
                        <em>Answer:</em>
                        <p v-html="item.detail.faq.answer.comment"></p>
                      </div>
                      <div class="faq-answer">
                        <p class="router" @click="goQuestion(item.detail.faq.question.id)">More Info</p>
                      </div>
                      <div class="action">
                        <span>Is this insight helpful?</span>
                        <span
                          :class="item.detail.faq.question.liked===1?'like-on':''"
                          @click="like(item.detail.faq.question,1)"
                          v-click-metric:ACE_QUESTION_LIKE="{name: 'like',question:item.detail.faq.question.title}"
                        >
                          <i class="zeta-icon-thumb-up" />
                        </span>
                        <span
                          :class="item.detail.faq.question.liked===-1?'like-on':''"
                          @click="like(item.detail.faq.question,-1)"
                          v-click-metric:ACE_QUESTION_LIKE="{name: 'unlike',question:item.detail.faq.question.title}"
                        >
                          <i class="zeta-icon-thumb-down" />
                        </span>
                      </div>
                    </div>
                    <div v-if="item.detail.table" class="table">
                      <TableView :data="item.detail.table" />
                    </div>
                    <div v-if="item.detail.udf" class="udf">
                      <UDFView :item="item" />
                    </div>
                    <div v-if="item.detail.spark" class="spark">
                      <div class="name">Spark grammar Content</div>
                      <ul>
                        <li v-for="s in item.detail.spark" :key="s.title">
                          <a :href="s.url" target="_blank">{{s.title}}</a>
                          <p>{{s.brief}}</p>
                        </li>
                      </ul>
                    </div>
                    <div v-if="item.detail.search" class="search">
                      <SuggestedTabView :resultHTML="item.detail.search" @click-item="handleHint"/>
                      <div class="submit-question-wrap">
                        <p class="submit-space"></p>
                        <span @click="handleHint(item.detail.search.newQ[0])" v-click-metric:ACE_CLICK="{name: 'needHelp'}"> Still need help? Submit a question.</span>
                      </div>
                    </div>
                  </div>
                  <div v-if="item.newQuestion" class="new-question-box">
                    <new-question-view :question="item.newQuestion" />
                  </div>
                  <p class="space" v-if="item.link&&item.detail" />
                  <p v-if="item.link" class="link">
                    <span>{{ item.link.title }}</span>
                    <a :href="item.link.href" target="_blank">{{ item.link.href }}</a>
                  </p>
                </div>
              </div>
              <p class="time">{{item.time}}</p>
            </div>
          </div>
          <div class="message-loading" v-show="loading">
            <div class="loading-box">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        </div>
        <div class="footer" :class="[sendActive?'show':'']">
          <div class="search-result" v-loading="hintLoading">
            <SuggestedView :resultHTML="resultHTML" @click-item="handleHint"/>
          </div>
          <div class="input-box">
            <input
              ref="input"
              v-model="input"
              type="text"
              class="message"
              placeholder="Search Table Name, UDF function, Spark SQL..."
              maxlength="200"
              @keyup.enter="send"
            />
            <i class="zeta-icon-submit send" :class="sendActive?'active':''" @click="send" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import {
  Component,
  Vue,
  Prop,
  Provide,
  Watch,
  Mixins
} from 'vue-property-decorator';
import _ from 'lodash';
import $ from 'jquery';
import moment from 'moment';
import Util from '@/services/Util.service';
import ZetaAceRemoteService from '@/services/remote/ZetaAce.ts';
import DoeRemoteService from '@/services/remote/DoeRemoteService.ts';
// import EnotifyMixin from './enotify-mixin';
import NotifyView from './type-view/notify-view.vue';
import { Message, TYPE } from '@/types/ace';
import TableView from './type-view/table-view.vue';
import UDFView from './type-view/udf-view.vue';
import SuggestedView from './type-view/suggested-view.vue';
import SuggestedTabView from './type-view/suggested-tab-view.vue';
import NewQuestionView from './type-view/new-question-view.vue';
import { ZetaAceMessage, allPlatform, isVDMTable, parseAcceptedAnswer } from './utilties';
import ACEMixin from './ace-mixin';
import './style.scss';
import * as Logger from '@/plugins/logs/zds-logger';
import { ZetaException } from '@/types/exception';
@Component({
  components: { TableView, UDFView, NotifyView, SuggestedView, SuggestedTabView, NewQuestionView }
})
export default class ZetaAce extends Mixins( ACEMixin ) {
  @Provide('zetaAceRemoteService')
  zetaAceRemoteService = new ZetaAceRemoteService();
  doeRemoteService = new DoeRemoteService();
  input = '';
  sendActive = false;
  list: Array<Message> = [];
  resultHTML: Dict<any> = {};
  debouncedSearch: Function;
  aceDefaultQuestion: [];
  loading = false;
  isDrag = false;
  platform = '';
  hintLoading = false;
  dragging = false;
  // lastClientY = 0;
  // lastClientX = 0;
  height = 300;
  width = 0;
  fullScreen = true;
  changePlatform = false;
  constructor() {
    super();
    this.debouncedSearch = _.debounce(this.getHint, 300, {
      trailing: true
    });
    this.aceDefaultQuestion = [];
  }
  mounted () {
    window.addEventListener('resize', this.handleResize, true);
    this.getDefaultQuestion();
  }
  handleResize(){
    const parentNode = document.getElementsByClassName(
      'zeta-ace-wrapper'
    )[0] as HTMLElement;
    const maxWidth = document.documentElement.clientWidth - 93;
    const maxHeight = document.documentElement.clientHeight - 50 - 93;
    parentNode.style.left = maxWidth + 'px';
    parentNode.style.top = maxHeight + 'px';
  }
  getDefaultQuestion(){
    this.zetaAceRemoteService.getDefaultQuestion().then((res)=>{
      if(res && res.data ){
        this.aceDefaultQuestion = res.data.questions.sort((q1,q2)=>{
          if(q2.pickUp === q1.pickUp){
            return q2.id - q1.id;
          }
          return q2.pickUp - q1.pickUp;
        });
      }
    }).catch((e: ZetaException)=>{
      e.resolve();
      console.error('get ace default question failed',e);
    });
  }
  get day(){
    return moment().format('MMM DD');
  }
  get time(){
    return moment().format('h:mm:ss a');
  }
  get style(){
    return {
      height: this.height + 'px',
      width: this.width + 'px'
    };
  }
  get bodyStyle(){
    const maxWidth = window.innerWidth - 200;
    const w = 850 > maxWidth ? maxWidth : 850;
    return {
      width: this.width!==0? this.width+'px' : (!this.fullScreen? w :'400px')
    };
  }
  get className(){
    if(this.visible){
      if(!this.fullScreen){
        return 'show full-screen';
      }
      return 'show';
    }
    return '';
  }
  get user() {
    return Util.getNt();
  }
  get aceNt() {
    return 'ace';
  }
  get messageList() {
    return this.list;
  }
  async sendDefaultQ(question: any) {
    const obj = {
      question: question.title
    };
    this.sendMsg(obj, this.user);
    this.loading = true;
    const questionItem = await this.getQuestion(question.id);
    const answer = Object.keys(questionItem).length>0?parseAcceptedAnswer(questionItem):'';
    const data = Object.assign({}, {
      title: '',
      question: '',
      detail: {
        faq: {
          question: questionItem,
          answer: answer
        }
      }
    });
    this.loading = false;
    this.receivedMsg(data);
  }
  receivedMsg(message: Message) {
    const data = new ZetaAceMessage(message);
    this.list.push(data);
  }
  sendMsg(message: Message, user: string) {
    const data = new ZetaAceMessage(message, user);
    this.list.push(data);
  }
  async send() {
    if (_.trim(this.input).length === 0) {
      return;
    }
    const obj = {
      question: this.input
    };
    this.sendMsg(obj, this.user);
    this.input = '';

    //search REST
    this.loading = true;
    const search = await this.search(obj.question);
    this.loading = false;
    const receivedMsg = {
      question: '',
      detail: {
        search
      }
    } as Message;
    this.receivedMsg(receivedMsg);
  }
  async handleHint(messageObj: any) {
    const obj = {
      question: messageObj.name
    };
    this.sendMsg(obj, this.user);
    this.input = '';
    //REST
    let data: Message;
    const { question } = obj;
    this.loading = true;
    const type = messageObj.searchType;
    if (type === 'Table') {
      const {
        metadataTable,
        tableArr,
        platformArr,
        platform,
        metadataView,
        viewArr
      } = await this.getMetaData(messageObj);

      data = Object.assign({}, obj, {
        title: '',
        question: '',
        detail: {
          table: {
            activeTab: 'overview',
            platform: platform,
            platformArr: platformArr,
            metadataTable: metadataTable,
            metadataView: metadataView,
            tableArr: tableArr,
            viewArr: viewArr,
            name: question.toUpperCase(),
            rowType: messageObj.rowType
          }
        }
      });
    } else if (type === 'UDF') {
      data = Object.assign({}, obj, {
        title: '',
        question: '',
        detail: {
          udf: messageObj
        }
      });
    } else if (type === 'FAQ') {
      const qid = messageObj.questionId || messageObj.id;
      const questionItem = await this.getQuestion(qid);
      const answer = Object.keys(questionItem).length>0?parseAcceptedAnswer(questionItem):'';
      data = Object.assign({}, obj, {
        title: '',
        question: '',
        detail: {
          faq: {
            question: questionItem,
            answer: answer
          }
        }
      });
    } else if (type === 'SparkSQL') {
      const result = await this.googleSearch(question);
      data = Object.assign({}, obj, {
        title: '',
        question: '',
        detail: {
          spark: result
        }
      });
    } else {
      data = Object.assign({}, obj, {
        title:'',
        question: '',
        newQuestion: {
          title: 'Submit a question',
          question: obj.question,
          imgURL: [],
          submit: false,
          error: '',
          loading: false
        },
      });
    }
    this.loading = false;
    this.receivedMsg(data);
  }
  async getHint(input?: string) {
    // this.resultHTML = {};
    const keyWords = _.trim(this.input || input);
    if (keyWords.length > 0) {
      this.hintLoading = true;
      this.resultHTML = await this.search(keyWords, true);
      this.hintLoading = false;
    }
  }
  async search(query: string, hint?: boolean) {
    let searchResult: Dict<any> = {};
    try {
      const getSearch = this.zetaAceRemoteService.aceSearch(query);
      const queryNotebook = this.zetaAceRemoteService.queryNotebook(query);
      const requestArr = [getSearch, queryNotebook];
      const result = await Promise.all(requestArr);
      const data = result[0].data;
      const notebook = result[1].data.query;
      data.query = notebook;
      let faq = result[0].data.faq||[];
      if(faq.length>0){
        faq = await this.getTagsByQuestions(faq);
      }
      searchResult = this.handleSearchResult(data, query, hint);
      return searchResult;
    } catch (err) {
      console.error(err);
      return searchResult;
    }
  }
  // question vote
  like(question: any, isLike: number) {
    if (
      (question.liked === 1 && isLike === 1) ||
      (question.liked === -1 && isLike === -1)
    )
      return;
    const qid = question.id;
    if (isLike === 1) {
      question.liked = 1;
    } else {
      question.liked = -1;
    }
    this.zetaAceRemoteService
      .questionVote(qid, isLike)
      .then(res => {
        // if(isLike === 1){
        //     question.liked = 1;
        // }else{
        //     question.liked = -1;
        // }
      })
      .catch(err => {
        if (isLike === 1) {
          question.liked = -1;
        } else {
          question.liked = 1;
        }
        console.error(err);
      });
  }
  getMetaData(table: any) {
    if(isVDMTable(table)){
      return this.getVDMInfo(table.name);
    }else{
      return this.getMetaTable(table.name);
    }
  }
  async getMetaTable(tableName: string){
    try {
      const result = await Promise.all([
        this.doeRemoteService.getMetaDataTable(tableName),
        this.doeRemoteService.getMetaDataView(tableName)
      ]);
      console.debug('Call Api:getMetaData success');
      let metadataTable: Array<any> = [];
      let platformArr: Array<any> = [];
      let metadataView: Array<any> = [];
      let tableArr: Array<any> = [];
      let viewArr: Array<any> = [];
      const resTable = result[0];
      if (resTable && resTable.data && resTable.data != null) {
        if (
          !_.isUndefined(resTable.data.data) &&
          resTable.data.data.hasOwnProperty('value')
        ) {
          metadataTable = _.orderBy(
            resTable.data.data.value,
            ['platform', 'db_name'],
            ['asc', 'desc']
          );
          platformArr = this.handlePlatformArr(metadataTable, metadataView);
          this.platform = !_.isEmpty(platformArr) ? platformArr[0] : '';
          const pickTable: any = _.pickBy(metadataTable, (v: any) => {
            return _.toLower(v.platform) == _.toLower(this.platform);
          });
          tableArr = pickTable;
        }
      }
      const resView = result[1];
      if (resView && resView.data && resView.data != null) {
        if (
          !_.isUndefined(resView.data.data) &&
          resView.data.data.hasOwnProperty('value')
        ) {
          metadataView = resView.data.data.value;
          platformArr = this.handlePlatformArr(metadataTable, metadataView);
          this.platform = !_.isEmpty(platformArr) ? platformArr[0] : '';
          const pickView: any = _.pickBy(metadataView, (v: any) => {
            return _.toLower(v.platform) == _.toLower(this.platform);
          });
          viewArr = pickView;
        }
      }
      return {
        metadataTable: metadataTable,
        tableArr: tableArr,
        platformArr: platformArr,
        platform: this.platform,
        metadataView: metadataView,
        viewArr: viewArr
      };
    } catch (err) {
      return {
        metadataTable: [],
        tableArr: [],
        platformArr: [],
        platform: this.platform,
        metadataView: [],
        viewArr: []
      };
    }
  }
  async getVDMInfo(tableName: string) {
    const paramsArr = tableName.split('.');
    const param = {
      db: paramsArr.length > 0 ? paramsArr[0] : '',
      table: paramsArr.length > 1 ? paramsArr[1] : ''
    };
    return this.doeRemoteService.getVDMInfo(param).then((res: any) => {
      console.debug('Call Api:getVDMInfo success');
      let metadataTable: Array<any> = [];
      let platformArr: Array<any> = [];
      const metadataView: Array<any> = [];
      let tableArr: Array<any> = [];
      const viewArr: Array<any> = [];
      if (res && res.data && res.data != null) {
        if (!_.isUndefined(res.data.data) && res.data.data.hasOwnProperty('value')) {
          metadataTable = _.orderBy(
            res.data.data.value,
            ['platform', 'db_name'],
            ['asc', 'desc']
          );
          platformArr = this.handlePlatformArr(metadataTable, metadataView);
          this.platform = !_.isEmpty(platformArr) ? platformArr[0] : '';
          const pickTable: any = _.pickBy(metadataTable, (v: any) => {
            return _.toLower(v.platform) == _.toLower(this.platform);
          });
          tableArr = pickTable;
        }
      }
      platformArr = this.handlePlatformArr(metadataTable, metadataView);
      return {
        metadataTable: metadataTable,
        tableArr: tableArr,
        platformArr: platformArr,
        platform: this.platform,
        metadataView: metadataView,
        viewArr: viewArr
      };
    }).catch(err => {
      console.error('Call Api:getVDMInfo failed: ' + JSON.stringify(err));
      return {
        metadataTable: [],
        tableArr: [],
        platformArr: [],
        platform: this.platform,
        metadataView: [],
        viewArr: []
      };
    });
  }
  handlePlatformArr(metadataTable: any, metadataView: any) {
    const rs: any = [];
    if (metadataTable && !_.isEmpty(metadataTable)) {
      const arr = _.uniq(_.map(metadataTable, 'platform'));
      _.forEach(arr, (v: any) => {
        rs.push(
          v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()).replace('mozart', 'Mozart')
        );
      });
    }

    if (metadataView && !_.isEmpty(metadataView)) {
      const arr = _.uniq(_.map(metadataView, 'platform'));
      _.forEach(arr, (v: any) => {
        rs.push(
          v.toLowerCase().replace(/( |^)[a-z]/g, (L: any) => L.toUpperCase()).replace('mozart', 'Mozart')
        );
      });
    }
    return _.intersection(allPlatform, _.uniq(rs));
  }
  goQuestion(id: number){
    this.$router.push(`/FAQDetail?id=${id}`);
  }
  changeVisible() {
    if (this.isDrag) {
      return;
    }
    this.setVisible(!this.visible);
  }
  scrollTop() {
    const messageList = document.getElementsByClassName('message-list');
    const lastMessage = messageList[messageList.length - 1] as HTMLElement;
    $('.content').animate({ scrollTop: lastMessage.offsetTop - 40 });
  }
  @Watch('input')
  handleMessage(newVal: string) {
    this.sendActive = _.trim(newVal).length > 0 ? true : false;
    if (this.sendActive) {
      this.debouncedSearch();
    } else {
      this.resultHTML = [];
    }
  }
  @Watch('list')
  handleList() {
    this.$nextTick(() => {
      this.scrollTop();
    });
  }
  get changeNotify() {
    const { visible, filterNotification, changePlatform } = this;
    return { visible, filterNotification, changePlatform };
  }
  @Watch('changeNotify')
  handleNotifyChange() {
    if (this.filterNotification.length > 0 && this.visible) {
      _.sortBy(this.filterNotification, ['createTime']).map(item => {
        const obj = {
          type: TYPE.ACE_ENOTIFY,
          detail: {
            notify: item
          },
          time: moment().format('h:mm:ss a')
        };
        this.receivedMsg(obj);
      });
      const ids: string[] = _.chain(this.filterNotification)
        .map(item => {
          return item.id;
        })
        .value();
      if (ids.length > 0) {
        this.readNotify(ids);
      }
    }
  }

  @Watch('visible')
  handleTracking(val: boolean) {
    if (val) {
      (this.$refs.input as HTMLElement).focus();
      Logger.counter('ACE_CLICK', 1, { name: 'open' });
      if(!this.fullScreen){
        const maxWidth = window.innerWidth - 200;
        this.width = 850 > maxWidth ? maxWidth : 850;
      }else{
        this.width = 400;
      }
    }else{
      this.width = 0;
    }
  }
  setVisible(val: boolean) {
    this.$store.dispatch('setAceVisible', val);
  }
  get visible() {
    return this.$store.state.zetaAceStore.visible;
  }
  // ace notification
  get defaultDataMoveQuestion() {
    const defaultQuestion = this.$store.getters.defaultQuestion;
    return defaultQuestion;
  }
  @Watch('defaultDataMoveQuestion')
  handleNotify(val: Array<any>) {
    if (val.length > 0) {
      this.sendMsg({question:val[0].question}, this.user);
      const obj = {
        type: TYPE.ACE_QUESTION,
        detail: {
          notify: {
            question: val[0].question,
            answer: val[0].answer.question
          }
        },
        time: moment().format('h:mm:ss a')
      };
      this.receivedMsg(obj);
    }
  }
}
</script>
