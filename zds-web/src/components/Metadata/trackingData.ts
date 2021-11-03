import _ from 'lodash';
import Util from '@/services/Util.service';

export interface SourceProps {
  transName: string;
  transType: string;
  user: string;
  module? : any;
}

export interface SearchProps {
  keywords: string;
  objs: any;
  result: any;
  offset: any;
  size: any;
  totalSize: any;
  index? : any;
  currentPage? : any;
  target_name? : any;
  target_type? : any;
}

export class TrackingData {
  data: any = {};

  constructor (source: SourceProps, data: SearchProps) {
    this.data['event.user'] = Util.getNt();
    this.data['event.type'] = source.transType;
    this.data['event.pageName'] = source.transName;
    this.data['event.module'] = source.module ? source.module : 'unknown';
    this.data['event.url'] = window.location.href;
    this.data['search.keywords'] = data.keywords;
    this.data['search.types'] = data.objs;
    this.data['search.resultList'] = data.result;
    this.data['search.offset'] = data.offset;
    this.data['search.pageSize'] = data.size;
    this.data['search.totalHit'] = data.totalSize;
    this.data['search.index'] = data.index;
    this.data['search.page'] = data.currentPage;
    this.data['search.targetName'] = data.target_name;
    this.data['search.targetType'] = data.target_type;
  }
}
