const Cookies = require('js-cookie');
import moment from 'moment';
import config from '@/config/config';
import Vue from 'vue';
import { ElLoadingComponent } from 'element-ui/types/loading';
import { IFile } from '@/types/repository';
import sqlFormatter from './sql-formatter/sqlFormatter.js';
import { ElMessageBoxOptions, MessageBoxCloseAction } from 'element-ui/types/message-box';
import { CodeType, IConnection } from '@/types/workspace';
// const sqlFormatter = require('./sql-formatter/sqlFormatter.js').default;

export default class Util {
  public static isDevelopment(): boolean {
    return process.env.NODE_ENV === 'development';
  }
  public static getResuestHeader(zetaToken = true) {
    const headers = {};
    if (zetaToken) {
      headers['Zeta-Authorization'] = 'Bearer ' + Util.getZetaToken();
    }
    return headers;
  }
  /* Zeta Token */
  public static getNt() {
    if (!config.webOSSOn && process.env.VUE_APP_PREDEFINE_NT) {
      return  process.env.VUE_APP_PREDEFINE_NT;
    }
    return window.dssAuthClient.getNT();
  }
  public static getCurrUserInfo() {
    let user = Cookies.get('ihub-user') || sessionStorage.getItem('ihub-user');
    try {
      user = JSON.parse(user);
    }
    catch(e) {
      user = {};
    }
    return user;
  }
  public static getCurrUserFullName() {
    const info = Util.getCurrUserInfo();
    try {
      if (Array.isArray(info)) {
        return info.find(i => i.PF_AUTH_DISPLAYNAME).PF_AUTH_DISPLAYNAME;
      } else {
        return info['PF_AUTH_DISPLAYNAME'];
      }
    } catch {
      return '';
    }
  }
  public static getDacToken() {
    if (!config.webOSSOn) return '000';
    return window.dssAuthClient.getToken();
  }
  public static getZetaToken() {
    return sessionStorage.getItem('zetaToken');
  }
  public static setZetaToken(token: string){
    sessionStorage.setItem('updateTime',moment().format('HH:mm YYYY-MM-DD'));
    return sessionStorage.setItem('zetaToken',token);
  }
  public static removeZetaToken() {
    sessionStorage.removeItem('zetaToken');
  }

  public static separateTableDB(fullName: string) {
    if (!fullName || fullName === '') {
      return null;
    }
    let table = '', db = '';
    const pointCnt = fullName.match(/\./ig);
    if (pointCnt && pointCnt.length != 1) {
    }
    else if(pointCnt && pointCnt.length === 1){
      table = fullName.split('.')[1];
      db = fullName.split('.')[0];
    }
    else if(!pointCnt){
      table = fullName,
      db = '';
    }
    return {
      db, table,
    };
  }
  public static SQLFormatter(script: string){
    if(script){
      return sqlFormatter.format(script, {
        language: 'sparksql',
      });
    }
    else{
      return '';
    }

  }
  public static UpperSQLReserved(script: string){
    if(script){
      return sqlFormatter.UpperSQLReserved(script, {
        language: 'sparksql',
      });
    }
    else{
      return '';
    }

  }

  public static getPathname(file: IFile) {
    console.warn(file.path);
    if (file.path.charAt(file.path.length - 1) !== '/') {
      console.error(file.path, file.notebookId);
    }
    const prefix = file.path + (file.path.charAt(file.path.length - 1) === '/' ? '' : '/');
    return prefix + file.title;
  }
  /* Service for singleton vue instance
     * These services are for external modules,
     * e.g. network module, that dont not depend vue
     */
  private static app: Vue;
  private static h_loading: ElLoadingComponent;

  public static register(app: Vue) {
    Util.app = app;
  }

  public static loading(on: boolean, text?: string) {
    if (!Util.app) {
      console.warn('Vue instance not registered globally. Cannot use fullscreen loading.');
      return;
    }

    if (on) {
      if (Util.h_loading) Util.h_loading.close();
      Util.h_loading = Util.app.$loading({
        lock: true,
        text: text || 'Loading...',
        background: 'rgba(255,255,255,0.7)',
      });
    }
    else {
      if (Util.h_loading) Util.h_loading.close();
    }
  }

  public static getApp(): Vue {
    return Util.app;
  }
  public static getOS(): string{
    const UserAgent = navigator.userAgent.toLowerCase();
    if(/windows nt/.test(UserAgent)){
      return 'WIN';
    }
    else if(/mac os/.test(UserAgent)){
      return 'MAC';
    }
    else{
      return 'OTHERS';
    }
  }
  public static registerBeforeunload () {
    window.onbeforeunload = e => {
      e.returnValue = 'Are you sure to exit Zeta Dev Suite? All of your unsaved work will be lost.';
      return e.returnValue;
    };
  }
  public static unregisterBeforeunload () {
    window.onbeforeunload = null;
  }
  public static reloadPage(forceRefresh = true) {
    Util.removeZetaToken();
    Util.unregisterBeforeunload();
    setTimeout(()=> window.location.reload(forceRefresh),30);
  }
  static tokenExpiredAlert = 0;
  public static refreshPopup(head: string, message: string, forceRefresh = true) {
    if(head == 'Token expire' && Util.tokenExpiredAlert > 0) {
      return;
    } else if (head == 'Token expire' && Util.tokenExpiredAlert == 0) {
      Util.tokenExpiredAlert++;
    }
    const cfm = Util.getApp().$confirm(message, head, {
      showCancelButton: false,
      confirmButtonText: 'Refresh',
      type: 'warning',
      /** disable user close to avoid some confusion */
      showClose:true,
      closeOnClickModal:false,
      closeOnPressEscape:false,
      beforeClose: (action: MessageBoxCloseAction, instance, done) => {
        if(action === 'confirm') {
          instance.confirmButtonLoading = true;
          instance.confirmButtonText = 'Refreshing';
          Util.reloadPage();
        }
        else {
          done();
        }
      },
    } as ElMessageBoxOptions).then(() => {
      Util.reloadPage();
    }).catch(() => {
      Util.tokenExpiredAlert--;
    });
  }
  public static getDefaultConnectionByPlat = (codeType: CodeType) => {
    let cnn: {
      defaultConnection: IConnection;
      [k: string]: any;
    } = config.zeta.notebook.connection;
    switch(codeType) {
      case CodeType.SQL:
        cnn = config.zeta.notebook.connection;
        break;
      case CodeType.TERADATA:
        cnn = config.zeta.notebook.tdConnection;
        break;
      case CodeType.KYLIN:
        cnn = config.zeta.notebook.kylinConnection;
        break;
      case CodeType.HIVE:
        cnn = config.zeta.notebook.hiveConnection;
        break;
      case CodeType.SPARK_PYTHON:
        cnn = config.zeta.notebook.pyConnection;
        break;
    }
    return cnn;
  };
  public static isShareApp = () => {
    return Util.getApp() ? Util.getApp().$appName === 'zeta-share' : false;
  };
  public static generateSessionId() {
    const nt = Util.getNt();
    const env = config.env;
    const timestamp = new Date().getTime();
    return `wsId-${nt}-${env}-${timestamp}`;
  }

  public static generateClientId() {
    const nt = Util.getNt();
    const env = config.env;
    const timestamp = new Date().getTime();
    return `cId-${nt}-${env}-${timestamp}`;
  }

  public static setClientId(id: string) {
    sessionStorage.setItem('clientId', id);
  }

  public static getClientId() {
    let cId = sessionStorage.getItem('clientId');
    if (!cId) {
      cId = Util.generateClientId();
      Util.setClientId(cId);
    }
    return cId;
  }
  public static getPath() {
    const path = process.env.BASE_URL || 'zeta';
    return `${path}/`;
  }

}
