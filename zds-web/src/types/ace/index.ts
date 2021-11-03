export enum TYPE {
  ACE_MESSAGE = 'ACE_MESSAGE',
  ACE_ENOTIFY = 'ACE_ENOTIFY',
  ACE_QUESTION = 'ACE_QUESTION'
}
export interface Message {
  type?: TYPE;
  title?: string;
  question?: string;
  input?: string;
  detail?: MessageDetail;
  action?: any;
  newQuestion?: any;
  link?: Link;
  path?: string;
  user?: string;
  day?: string;
  time?: string;
}
export interface MessageDetail {
  list?: Array<any>;
  table?: any;
  udf?: any;
  faq?: any;
  search?: any;
  notify?: any;
  spark?: any;
}
export interface Link {
  title?: string;
  href: string;
}
export interface FaqRes extends FaqBase {
  title: string;
  content: string;
  submitter: string;
  posts: Array<Post>;
  answers: Array<Post>;
  totalPost: number;
  totalLike: number;
  totalPostLike: number;
  totalLevel1Post: number;
  tags?: Array<any>;
}

export interface Post extends FaqBase {
  questionId: number;
  comment: string;
  poster: string;
  replyTo: number | null;
  totalLike: number;
  accepted: boolean;
  showTextarea?: boolean;
  edit?: boolean;
  replyDetail: {
    showMore: false;
    list: Array<Post>;
  };
  [key: string]: any;
}
export interface Like extends FaqBase {
  nt: string;
  postId: number;
  questionId: number;
  unlike: boolean;
}
interface FaqBase {
  id: number;
  liked: number;
  createTime: string;
  updateTime: string;
}

export interface Tag {
  createTime: string;
  description: string;
  id: number;
  name: string;
  nt: string;
}
export interface EnotifyType {
  'GENERAL': 'GENERAL';
  'PRODUCT': 'PRODUCT';
  'MAINTENANCE': 'MAINTENANCE';
}
export interface EnotifyItem{
  createTime: string;
  id: number;
  link: string;
  product: string;
  read: boolean;
  readTime: string | null;
  title: string;
  type: EnotifyType;
  startTime?: string;
  endTime?: string;
}
export enum EnotifyProduct {
  'Ares'= 'Ares',
  'Hermes'= 'Hermes_RNO',
  'ApolloReno'= 'Apollo_RNO',
  'Hercules'= 'Hercules',
  'HerculesSub'= 'Hercules',
  'Hopper'= 'Hopper',
  'Mozart'= 'Mozart',
  'NuMozart'= 'Mozart',
  'Kylin-rno'= 'Kylin',
  'Kylin-rno-qa'= 'Kylin',
  'Zeta'= 'Zeta',
}
export interface Enotify{
  createTime: string;
  id: number;
  link: string;
  product: EnotifyProduct;
  read?: boolean;
  readTime?: string | null;
  title: string;
  type: EnotifyType;
  startTime?: string;
  endTime?: string;
}
export interface SimpleNotify {
  content: string;
}
export enum NotifyType {
  'ENOTIFY'= 'enotify',
  'SIMPLE'='simple',
}
export interface Notification {
  id: string;
  creator: string;
  type: NotifyType;
  body: Enotify | SimpleNotify;
  createAt: string;
  updateAt: string;
  [key: string]: any;
}
