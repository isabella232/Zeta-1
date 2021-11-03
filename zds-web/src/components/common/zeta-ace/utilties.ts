import { Message, MessageDetail, Link, TYPE } from '@/types/ace';
import moment from 'moment';
import _ from 'lodash';
import Util from '@/services/Util.service';
import metadataConf from '@/components/WorkSpace/Metadata/metadata-config';
export const allPlatform = metadataConf.allPlatform;
export const allPlatformLowercase = metadataConf.allPlatformLowercase;
export function getPlatformIconClass(platform: string){
  if(_.toLower(platform).indexOf('apollo') >=0 ){
    return 'apollo';
  }
  return _.toLower(platform);
}
export function isVDMTable(meta: any) {
  if (meta.rowType && meta.rowType.indexOf('vdm')>-1) {
    return true;
  }
  return false;
}
export function openTab(meta: any){
  const type: string = isVDMTable(meta)?'vdm':'';
  const url= `${location.protocol}//${location.host}/${Util.getPath()}share/#/metadata?tableName=${meta.name}`
  + (type?`&type=${type}`:'');
  window.open(url);
}
export function openNotebookTab(item: any){
  if (item.type === 'zeppelin') {
    const url = `${location.protocol}//${location.host}/${Util.getPath()}share/#/notebook/multilang?notebookId=${item.id}`;
    window.open(url,'_blank');
  } else {
    const url = `${location.protocol}//${location.host}/${Util.getPath()}share/#/notebook?notebookId=${item.id}`;
    window.open(url,'_blank');
  }
}
export function parseAcceptedAnswer(question: any){
  const accepted = _.chain(question.posts).filter(p =>{return p.accepted === true;}).value();
  if(accepted.length>0){
    return accepted[0];
  }
  const posts = _.chain(question.posts)
    .sort((p1: any,p2: any) => {
      if(p2.totalLike === p1.totalLike){
        return moment(p1.createTime).valueOf() - moment(p2.createTime).valueOf();
      }
      return p2.totalLike - p1.totalLike;
    })
    .value();
  const post = posts[0] ? posts[0] : {};
  return post;
}
export const defaultData: Array<Message> = [
  {
    title: 'Hi, I’m Zeta Ace, what’s new?',
    detail: {
      list: [
        {
          question: '(Important) Request Yubi-key for Zeta access before June 30th',
          answer: {
            question: "",
            link: {
              title: "Please refer this link:",
              href: "https://enotify.corp.ebay.com/api/general/html/196067"
            }
          }
        },
        {
          question: 'How to requset view access on Hermes. (new)',
          answer: {
            question: "Hermes currently can only allow user access view instead of table.\n If you have access issue, please refer this wiki.\n <a href='https://wiki.vip.corp.ebay.com/display/GDI/Access+Provisioning+in+Hadoop' target='_blank'>https://wiki.vip.corp.ebay.com/display/GDI/Access+Provisioning+in+Hadoop</a>.",
            link: {
              title: "Here’s FAQ list:",
              href: "https://wiki.vip.corp.ebay.com/display/BTD/FAQ"
            }
          }
        },
        {
          question: "Bundle Access Request",
          answer: {
            question: "Currently, Hadoop allow user request bundle access like experience for Teradata.\n Please go to bdp(<a href='https://bdp.vip.ebay.com/' target='_blank'>https://bdp.vip.ebay.com/</a>) -> ‘Bundle Access’.",
          }
        },
        {
          question: "Package pre-installed for PySpark and Spark R",
          answer: {
            question: "<a href='https://wiki.vip.corp.ebay.com/x/fcQPKQ' target='_blank'>https://wiki.vip.corp.ebay.com/x/fcQPKQ</a>",
          }
        }
      ]
    },
    day: moment().format('MMM DD'),
    time: moment().format('h:mm:ss a')
  }
];
export const defaultDataMoveQuestion: Array<any> = [
  {
    question: "Data move - No write access to HDM(Hadoop data mart) on Hermes",
    answer: {
      question: "Two potential reasons for showing this issue.\n 1.User don’t have Hermes access\n&nbsp;&nbsp;&nbsp;&nbsp;Please go this like to request Hermes access on <a href='https://bdp.vip.ebay.com' target='_blank'>https://bdp.vip.ebay.com</a>.\n 2.You have no Hermes database created.\n &nbsp;&nbsp;&nbsp;&nbsp;Create & Manage your workspace on <a href='https://bdp.vip.ebay.com' target='_blank'>https://bdp.vip.ebay.com.</a>.",
    },
    time: moment().format('h:mm:ss a')
  }
];
export class ZetaAceMessage implements Message {
  user: string;
  type: TYPE;
  title?: string;
  question?: string;
  input?: string;
  detail?: MessageDetail;
  action?: any;
  link?: Link;
  path?: string;
  day?: string;
  time: string;
  constructor(msg: Message, user = 'ace') {
    Object.assign(this, msg);
    this.type = msg.type ? msg.type : TYPE.ACE_MESSAGE;
    this.time = moment().format('h:mm:ss a');
    this.user = user;
  }

}
