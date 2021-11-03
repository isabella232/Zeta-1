/* eslint-disable @typescript-eslint/camelcase */
import ESBaseRemoteService from '@/services/remote/ESBaseRemoteService';
import * as Axios from 'axios';
import config from '@/config/config';
import abbreviations from '@/config/abbreviations';
import _ from 'lodash';
import { MetadataDetail, SampleQuery } from '@/components/WorkSpace/Metadata';
import { UDFMetadata } from '@/components/Metadata/components/types';
export default class MetadataRemoteService extends ESBaseRemoteService {
  generateQuery (query: string): any {
    query = query.trim();
    if (!query) {
      return {
        query: {
          bool: {
            must: [{ wildcard: { name: '*' + query + '*' } }],
            must_not: [],
            should: []
          }
        },
        from: 0,
        size: 50,
        sort: [{ total_score: { order: 'desc' } }],
        aggs: {},
        version: true
      };
    }
    const queryArr = _.split(query, /\ |_/);
    const should: any = [];
    const nNameMust: any = [];
    const tagMust: any = [];
    const descMust: any = [];
    const map: any = abbreviations;
    _.forEach(queryArr, (v: string) => {
      const item = map && map[v] && !map[v].includes(' ') ? map[v] : v;
      nNameMust.push({ match: { 'normal name': item } });
      tagMust.push({ match: { tags: item } });
      descMust.push({ match: { desc: item } });
    });
    //should.push({ "term": { "name": { "value": map[query] ? map[query] : query, "boost": 1 } } });
    should.push({ wildcard: { name: { value: `*${query}*`, boost: 3 } } });
    should.push({ bool: { must: nNameMust, boost: 2 } });
    should.push({ bool: { must: tagMust, boost: 1 } });
    should.push({ wildcard: { desc: { value: `*${query}*`, boost: 1 } } });
    should.push({ bool: { must: descMust, boost: 0.5 } });
    //should.push({ "wildcard": { "desc": { "value": `*${query}*`, "boost": 0.1 } } });
    return {
      query: {
        function_score: {
          query: {
            bool: {
              should: should
            }
          },
          script_score: {
            script: {
              source: 'Math.log(doc[\'total_score\'].value + 1) + 2.5 * Math.log(_score + 1)'
            }
          }
        }
      },
      size: 50
    };
  }
  generateUpdate (query: string, desc: string): any {
    return {
      script: {
        source: 'ctx._source.desc=params.desc',
        params: {
          desc: desc
        }
      },
      query: {
        term: {
          name: {
            value: query
          }
        }
      }
    };
  }
  generateTableauQuery (query: string): any {
    query = query.trim();
    if (!query) {
      return {
        query: {
          bool: {
            must: [{ wildcard: { asset_name: '*' + query + '*' } }],
            must_not: [],
            should: []
          }
        },
        from: 0,
        size: 50,
        sort: [{ total_score: { order: 'desc' } }],
        aggs: {},
        version: true
      };
    }

    const queryArr = _.split(query, '&');
    const should: any = [];
    _.forEach(queryArr, v => {
      should.push({
        term: {
          asset_name: {
            value: v.trim(),
            boost: 10
          }
        }
      });
      should.push({
        wildcard: {
          asset_name: {
            value: `*${v.trim()}*`,
            boost: 1
          }
        }
      });
      should.push({
        term: {
          prj_name: {
            value: v.trim(),
            boost: 10
          }
        }
      });
      should.push({
        wildcard: {
          prj_name: {
            value: `*${v.trim()}*`,
            boost: 1
          }
        }
      });
      should.push({
        term: {
          parent_loc: {
            value: v.trim(),
            boost: 5
          }
        }
      });
      should.push({
        wildcard: {
          parent_loc: {
            value: `*${v.trim()}*`,
            boost: 0.5
          }
        }
      });
      should.push({
        term: {
          child_loc: {
            value: v.trim(),
            boost: 5
          }
        }
      });
      should.push({
        wildcard: {
          child_loc: {
            value: `*${v.trim()}*`,
            boost: 0.5
          }
        }
      });
    });

    return {
      query: {
        function_score: {
          query: {
            bool: {
              should: should
            }
          },
          script_score: {
            script: {
              source: 'Math.log(doc[\'user_cnt\'].value * _score + 1) + _score'
            }
          }
        }
      },
      size: 50
    };
  }

  /**
   * override REST method
   * @param url
   * @param config
   * @param canceler
   */
  get (url: string, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler) {
    return super.get(url, config, canceler, true);
  }
  post (url: string, data?: any, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler) {
    return super.post(url, data, config, canceler, true);
  }
  put (url: string, data?: any, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler) {
    return super.put(url, data, config, canceler, true);
  }
  queryMetadataList (query: string) {
    return this.post(`${config.zeta.esServer}metadata-es/metadata_tv_summary/_search`, this.generateQuery(query))
      .then(this.responseHandler)
      .then(this.parseResult);
  }
  queryTableDetail (name: string, isShare = false) {
    if (isShare) {
      return this.get(`${config.zeta.base}share/tables?name=${name}`).then(this.responseHandler);
    } else {
      return super.get(`${config.zeta.base}doemetadata/tables?name=${name}`).then(this.responseHandler);
    }
  }
  queryTableDetailFromDoe (name: string) {
    return this.get(`${config.doe.url}api/asset/tableInfoService?table=${name}`).then(this.responseHandler);
  }
  updateTableDetail (metadata: MetadataDetail[]) {
    return super.put(`${config.zeta.base}doemetadata/tables`, metadata).then(this.responseHandler);
  }
  queryViewDetail (name: string, isShare = false) {
    if (isShare) {
      return this.get(`${config.zeta.base}share/views?name=${name}`).then(this.responseHandler);
    } else {
      return super.get(`${config.zeta.base}doemetadata/views?name=${name}`).then(this.responseHandler);
    }
  }
  updateViewDetail (metadata: MetadataDetail[]) {
    return super.put(`${config.zeta.base}doemetadata/views`, metadata).then(this.responseHandler);
  }
  querySampleQuery (name: string, isShare = false) {
    if (isShare) {
      return this.get(`${config.zeta.base}share/sampleQueries/${name}`).then(this.responseHandler);
    } else {
      return super.get(`${config.zeta.base}doemetadata/sampleQueries/${name}`).then(this.responseHandler);
    }
  }
  updateSampleQuery (queries: SampleQuery[]) {
    return super.put(`${config.zeta.base}doemetadata/sampleQueries`, queries).then(this.responseHandler);
  }
  updateESTableDesc (query: string, desc: string) {
    return this.post(
      `${config.zeta.esServer}metadata-es/metadata_tv_summary/_update_by_query`,
      this.generateUpdate(query, desc)
    ).then(this.responseHandler);
  }
  putESTable (params: any, data: any) {
    return this.post(`${config.doe.doeService}es/push?obj=${params.obj}&id=${params.id}`, data).then(
      this.responseHandler
    );
  }
  queryTableauList (query: string) {
    return this.post(`${config.zeta.esServer}metadata-es/metadata_tableau/_search`, this.generateTableauQuery(query))
      .then(this.responseHandler)
      .then(this.parseResult);
  }

  createESUDF (data: UDFMetadata) {
    return this.post(`${config.doe.doeService}es/push?obj=udf&id=${data.db_name}.${data.name}`, data).then(this.responseHandler);
  }
}
