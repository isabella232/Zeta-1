import { ZetaException, ZetaExceptionProps, ZETA_EXCEPTION_TAG } from '@/types/exception';
import * as Axios from 'axios';
import Util from '../Util.service';
import config from '@/config/config';
import { EditableUDFMetadata, UDFMetadata } from '@/components/Metadata/components/types';
import DssAPI from './dss-api-remote-service';

export default class DoeRemoteService extends DssAPI {
  static readonly instance = new DoeRemoteService();

  defaultErrorHandler (error: any, url: string, props: ZetaExceptionProps = { path: 'unknown' }): ZetaException {
    console.error('DOE REST ERR: ', error);
    const data: any = {
      code: '',
      errorDetail: {
        message: error.response.data,
        cause: {
          cause: error.stack,
          errorCode: '',
          message: '',
          stackTrace: '',
        },
        rule: '',
      },
    };
    const exception = new ZetaException(data, props).tags([ZETA_EXCEPTION_TAG.REST_ERR]);
    Util.getApp().$store.dispatch('addException', { exception });
    return exception;
  }
  get (url: string, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler) {
    return super.get(url, config, canceler, false);
  }
  post (url: string, data?: any, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler) {
    return super.post(url, data, config, canceler, false);
  }
  checkDARole = (nt: string) => {
    return this.post(config.doe.url + 'asset/checkDARole', { cre_user: nt });
  };

  getModelList = (params: any) => {
    return this.post(config.doe.url + 'asset/getModelList', params);
  };

  getModel = (params: any) => {
    return this.post(config.doe.url + 'asset/getModel', params);
  };

  getPrevModel = (params: any) => {
    return this.post(config.doe.url + 'asset/getPrevModel', params);
  };

  addModel = (params: any) => {
    return this.post(config.doe.url + 'asset/addModel', params);
  };

  approveModel = (params: any) => {
    return this.post(config.doe.url + 'asset/approveModel', params);
  };

  addModelComment = (params: any) => {
    return this.post(config.doe.url + 'asset/addModelComment', params);
  };

  getSA = () => {
    return this.get(config.doe.url + 'asset/SAInfo');
  };

  getEmailAddr = (params: any) => {
    return this.get(config.doe.url + 'asset/LDAPInfo?nt=' + params);
  };

  createEmailStatus = (params: any) => {
    return this.get('https://zeta.dss.vip.ebay.com/defensor/Email/' + params);
  };

  createEmail = (params: any) => {
    return this.post('https://zeta.dss.vip.ebay.com/defensor/Email/create', params);
  };

  getMetaDataTable = (table: string) => {
    return this.get(config.doe.url + 'asset/tableInfoService?table=' + table + '&zeta=1');
  };

  getMetaDataView = (view: string) => {
    return this.get(config.doe.url + 'asset/viewInfoService?view=' + view + '&zeta=1');
  };

  addTblDesc = (table: string, desc_html: string) => {
    return this.post(config.doe.url + 'asset/addTblDesc', {
      desc: desc_html.replace(/<[^>]*>|/g, ''),
      desc_html,
      table,
      upd_user: Util.getNt(),
    });
  };

  addColDesc = (params: any) => {
    return this.post(config.doe.url + 'asset/addColDesc', params);
  };

  addSampleQuery = (params: any) => {
    return this.post(config.doe.url + 'asset/addSampleQueryById', params);
  };

  getSampleQuery = (params: any) => {
    return this.post(config.doe.url + 'asset/getSampleQuery', params);
  };

  getSampleData = (params: any) => {
    return this.post(config.doe.url + 'asset/getSampleData', params);
  };

  addSampleData = (params: any) => {
    return this.post(config.doe.url + 'asset/addSampleData', params);
  };

  getTableComment = (params: any) => {
    return this.post(config.doe.url + 'asset/getTableComments', params);
  };

  addTableComment = (params: any) => {
    return this.post(config.doe.url + 'asset/addTableComments', params);
  };

  getUser = (params: any) => {
    return this.get(config.doe.url + 'solr/typeAhead?core=user&q=' + params + '&type=user');
  };

  getUserByNT = (nt: string) => {
    return this.get(`${config.doe.url}solr/get`, {
      params: {
        core: 'ldap',
        nt,
      },
    }).then(resp => resp.data);
  };

  getEmail = (params: any) => {
    return this.get(config.doe.url + 'solr/typeAhead?core=ldap&q=' + params + '&type=user');
  };

  getVDMInfo = (params: any) => {
    return this.post(config.doe.url + 'asset/VDMInfoService', params);
  };

  registerVDM = (params: any) => {
    return this.post(config.doe.doeService + 'vdm/registerVDM', params);
  };

  checkVDM = (params: any) => {
    return this.post(config.doe.url + 'asset/checkVDM', params);
  };

  getVDMColumns = (params: any) => {
    return this.post(config.doe.url + 'asset/getVDMColumns', params);
  };

  addVDMDesc = (params: any) => {
    return this.post(config.doe.url + 'asset/addVDMDesc', params);
  };

  getDomainInfo = () => {
    return this.get(config.doe.url + 'asset/getDomainInfo');
  };

  getSubDomainInfo = () => {
    return this.get(config.doe.url + 'asset/getSubDomainInfo');
  };

  getTableListByDomain = (params: any) => {
    return this.post(config.doe.url + 'asset/getTableListByDomain', params);
  };

  getTableListBySubDomain = (params: any) => {
    return this.post(config.doe.url + 'asset/getTableListBySubDomain', params);
  };

  getTableCntByDomain = () => {
    return this.get(config.doe.url + 'asset/getTableCntByDomain');
  };

  getAllPlatfomsTableByThirtyDays = () => {
    return this.get(config.doe.url + 'asset/getAllPlatfomsTableByThirtyDays');
  };

  getDomainUsageVisit = () => {
    return this.get(config.doe.url + 'asset/getDomainUsageVisit');
  };

  getTableDailyUsageVisitByThirtyDays = (params: any) => {
    return this.post(config.doe.url + 'asset/getTableDailyUsageVisitByThirtyDays', params);
  };

  insertSearchHist = (params: any) => {
    return this.post(config.doe.url + 'asset/insertSearchHist', params);
  };

  getSearchHist = (params: any) => {
    return this.post(config.doe.url + 'asset/getSearchHist', params);
  };

  getSmartQuery = (params: any) => {
    return this.post(config.doe.url + 'asset/getSmartQuery', params);
  };

  getSearchTables = (params: { tables: string }) => {
    return this.post(config.doe.url + 'asset/getSearchTables', params);
  };

  getColumns = (params: any) => {
    return this.get(
      config.doe.url +
        'asset/getColumns?platform=' +
        params.platform +
        '&databasename=' +
        params.db +
        '&tablename=' +
        params.table
    );
  };

  getNews = () => {
    return this.get(config.doe.url + 'asset/getNews');
  };

  getRecommendationSearch = () => {
    return this.get(config.doe.url + 'asset/getRecommendationSearch');
  };

  getVdmDB = (params: any) => {
    return this.post(config.doe.url + 'asset/getVdmDB', params);
  };

  getVdmTable = (params: any) => {
    return this.post(config.doe.url + 'asset/getVdmTable', params);
  };

  getEnotify = (params: any) => {
    return this.post(config.doe.url + 'asset/getEnotify', params);
  };

  deleteSampleQuery = (params: any) => {
    return this.post(config.doe.url + 'asset/deleteSampleQuery', params);
  };

  deleteTableComments = (params: any) => {
    return this.post(config.doe.url + 'asset/deleteTableComments', params);
  };

  getEnotifyByType = (params: any) => {
    return this.post(config.doe.url + 'asset/getEnotifyByType', params);
  };

  getDomainComments = (params: any) => {
    return this.post(config.doe.url + 'asset/getDomainComments', params);
  };

  addDomainComments = (params: any) => {
    return this.post(config.doe.url + 'asset/addDomainComments', params);
  };

  deleteDomainComments = (params: any) => {
    return this.post(config.doe.url + 'asset/deleteDomainComments', params);
  };

  deleteReplyComments = (params: any) => {
    return this.post(config.doe.url + 'asset/deleteReplyComments', params);
  };

  updateDomainComment = (params: any) => {
    return this.post(config.doe.url + 'asset/updateDomainComment', params);
  };

  updateDomainReply = (params: any) => {
    return this.post(config.doe.url + 'asset/updateDomainReply', params);
  };

  getDomainConf = (params: any) => {
    return this.post(config.doe.url + 'asset/getDomainConf', params);
  };

  updateDomainConf = (params: any) => {
    return this.post(config.doe.url + 'asset/updateDomainConf', params);
  };

  replaceTableLabels = (table_name: string, labels: string[] | string) => {
    return this.post(config.doe.url + 'asset/replaceTableLabels', {
      table_name,
      labels: Array.isArray(labels) ? labels.join(',') : labels,
      nt: Util.getNt(),
    });
  };

  getTableLabels = (table_name: string) => {
    return this.post(config.doe.url + 'asset/getTableLabels', { table_name, 'nt': Util.getNt() });
  };

  getEnotifyByDomain = (params: any) => {
    return this.post(config.doe.url + 'asset/getEnotifyByDomain', params);
  };

  getDomainProduct = (params: any) => {
    return this.post(config.doe.url + 'asset/getDomainProduct', params);
  };

  insertDomainProduct = (params: any) => {
    return this.post(config.doe.url + 'asset/insertDomainProduct', params);
  };

  delDomainProduct = (params: any) => {
    return this.post(config.doe.url + 'asset/delDomainProduct', params);
  };

  getSubDomainProductOption = (params: any) => {
    return this.post(config.doe.url + 'asset/getSubDomainProductOption', params);
  };

  getDomainDesc = (params: any) => {
    return this.post(config.doe.url + 'asset/getDomainDesc', params);
  };

  updateDomainDesc = (params: any) => {
    return this.post(config.doe.url + 'asset/updateDomainDesc', params);
  };

  updateSubdomainDesc = (params: any) => {
    return this.post(config.doe.url + 'asset/updateSubdomainDesc', params);
  };

  addTblOwner = (params: any) => {
    return this.post(config.doe.url + 'asset/addTblOwner', params);
  };

  getProductionSource = (params: any) => {
    return this.post(config.doe.url + 'asset/getProductionSource', params);
  };

  getProductionTarget = (params: any) => {
    return this.post(config.doe.url + 'asset/getProductionTarget', params);
  };

  getProductionSourceTarget = (params: any) => {
    return this.post(config.doe.url + 'asset/getProductionSourceTarget', params);
  };

  getMainSource = (params: any) => {
    return this.post(config.doe.url + 'asset/getMainSource', params);
  };

  getMainTarget = (params: any) => {
    return this.post(config.doe.url + 'asset/getMainTarget', params);
  };

  getMainSourceTarget = (params: any) => {
    return this.post(config.doe.url + 'asset/getMainSourceTarget', params);
  };

  getConfidenceScore = (table: string) => {
    return this.post(config.doe.url + 'asset/getConfidenceScore', { table });
  };

  getTableInfo = (params: any) => {
    return this.get(
      config.doe.url +
        'asset/tableInfoService?table=' +
        params.table +
        '&platform=' +
        params.platform +
        '&db=' +
        params.db +
        '&zeta=1'
    );
  };

  esSearch = (params: any) => {
    return this.get(config.doe.doeService + 'es/search', { params });
  };

  esNameCompletion = (params: any) => {
    return this.get(config.doe.doeService + 'es/getNameCompletion', { params });
  };

  getDatasets = () => {
    return this.get(config.doe.url + 'asset/getDatasets');
  };

  getTableauConfidenceScore = (params: any, cancelToken?: any) => {
    return this.post(config.doe.url + 'asset/getTableauConfidenceScore', params, {
      cancelToken: cancelToken,
      headers: {
        'Content-Type': 'application/json; charset=utf-8',
      },
    });
  };
  getTableauImage = (params: any, cancelToken?: any) => {
    return this.get(config.doe.doeService + 'es/getTableauImage', {
      params,
      cancelToken,
    });
  };
  getRecommendationSearchHist = (params: any) => {
    return this.post(config.doe.url + 'asset/getRecommendationSearchHist', params);
  };

  getContactList = (params: any) => {
    return this.get(
      config.doe.url +
        'usage/getContactList?table=' +
        params.table +
        '&startDate=' +
        params.startDate +
        '&endDate=' +
        params.endDate
    );
  };

  getServerCurrent = () => {
    return this.get(config.doe.url + 'asset/getServerCurrent');
  };

  getSelfModelList = (params: any) => {
    return this.post(config.doe.url + 'asset/getSelfModelList', params);
  };

  getModelByMultiPltfrm = (params: any) => {
    return this.post(config.doe.url + 'asset/getModelByMultiPltfrm', params);
  };

  addModelByMultiPltfrm = (params: any) => {
    return this.post(config.doe.url + 'asset/addModelByMultiPltfrm', params);
  };

  deleteModelByMultiPltfrm = (params: any) => {
    return this.post(config.doe.url + 'asset/deleteModelByMultiPltfrm', params);
  };

  getModelSaOwner = (params: any) => {
    return this.get(config.doe.url + 'asset/getModelSaOwner?sa=' + params);
  };

  approveModelByMultiPltfrm = (params: any) => {
    return this.post(config.doe.url + 'asset/approveModelByMultiPltfrm', params);
  };

  getPrevModelByMultiPltfrm = (params: any) => {
    return this.post(config.doe.url + 'asset/getPrevModelByMultiPltfrm', params);
  };

  addModelCommentByMultiPltfrm = (params: any) => {
    return this.post(config.doe.url + 'asset/addModelCommentByMultiPltfrm', params);
  };

  getModelListByMultiPltfrm = (params: any) => {
    return this.post(config.doe.url + 'asset/getModelListByMultiPltfrm', params);
  };

  getUDFList = (params: any) => {
    return this.get(config.doe.url + 'udf/getUDFList', params);
  };

  getUDFByName = (name: string, db = 'default') => {
    return this.get(config.doe.url + 'udf/getUdfByName', {
      params: { name, db_name: db },
    });
  };
  createUDF = (udf: UDFMetadata) => {
    return this.post(config.doe.url + 'udf/createUDF', udf);
  };
  updateUDF = (udf: EditableUDFMetadata) => {
    return this.post(config.doe.url + 'udf/updateUDF', udf);
  };
  getDA () {
    return this.get(config.doe.doeService + 'sa/getDA').then(resp => resp.data);
  }
  getSAList = () => {
    return this.get(config.doe.doeService + 'sa/getSAList').then(resp => resp.data);
  };
  getSADetail = (sa: string) => {
    return this.get(config.doe.doeService + 'sa/getSA?sa=' + sa).then(resp => resp.data.data);
  };
  getDBList = (query?: string, isProduction?: 0 | 1, isWorking?: 0 | 1) => {
    const params = {
      dbName: query,
      prdctnFlag: isProduction,
      wrkngFlag: isWorking,
    };
    return this.get(config.doe.doeService + 'metadata/getDBList', { params }).then(resp => resp.data.data);
  };
  updateSADetail = (form: any) => {
    return this.post(config.doe.doeService + 'sa/saveSA', form);
  };

  getTopics = () => {
    return this.get(config.doe.url + 'topic/getTopics').then(resp => resp.data.data);
  };
  getTopicByName = (name: string) => {
    return this.get(config.doe.url + 'topic/getTopicByName?name=' + name).then(resp => resp.data);
  };
  getTopicFields = (name: string) => {
    return this.get(config.doe.url + 'topic/getTopicFields?name=' + name).then(resp => resp.data);
  };
  getADPOColumnValues = (params: any) => {
    return this.get(config.doe.url + 'table/getADPO_column_values?' + params);
  };

  getTableManagement = (params: any) => {
    return this.post(config.doe.doeService + 'table_management/getTableManagement', params).then(res => res.data);
  };

  getTableManagementPageSort = (params: any) => {
    return this.post(config.doe.doeService + 'table_management/getTableManagementPageSort', params).then(res => res.data);
  };

  getTableManagementColumnValues = (params: any) => {
    return this.get(config.doe.doeService + 'table_management/getTableManagementColumnValues?' + params);
  };

  updateTableManagement = (params: any) => {
    return this.post(config.doe.doeService + 'table_management/updateTableManagement', params);
  };

  getAllTableManagement = (params: any) => {
    return this.post(config.doe.doeService + 'table_management/getAllTableManagement', params).then(res => res.data);
  };

  getApolloMetadata = (params: any) => {
    return this.get(config.doe.url + 'table/getApolloMetadata?' + params);
  };

  getMozartMetadata = (params: any) => {
    return this.get(config.doe.url + 'table/getMozartMetadata?' + params);
  };

  getChangeLog = (params: any) => {
    return this.post(config.doe.url + 'table/getChangeLog', params);
  };

  getChangeLogPageSort = (params: any) => {
    return this.post(config.doe.url + 'table/getChangeLogPageSort', params);
  };

  updateApolloMetadata = (params: any) => {
    return this.post(config.doe.url + 'table/updateApolloMetadata', params);
  };

  updateMozartMetadata = (params: any) => {
    return this.post(config.doe.url + 'table/updateMozartMetadata', params);
  };

  updateApolloDQ = (params: any) => {
    return this.post(config.doe.url + 'table/updateApolloDQ', params);
  };

  updateMozartDQ = (params: any) => {
    return this.post(config.doe.url + 'table/updateMozartDQ', params);
  };

  updateApolloView = (params: any) => {
    return this.post(config.doe.url + 'table/updateApolloView', params);
  };

  updateMozartView = (params: any) => {
    return this.post(config.doe.url + 'table/updateMozartView', params);
  };

  getDQStatus = (table: string) => {
    return this.get(config.doe.url + 'operation/getDQStatus?table=' + table.toLowerCase()).then(res => res.data);
  };

  getBatchAccount = (table: string) => {
    return this.get(config.doe.url + 'operation/getBatchAccount?table=' + table.toLowerCase()).then(res => res.data);
  };

  getTableOperationInfo = (table: string) => {
    return this.get(config.doe.url + 'operation/getTableStatus?table=' + table.toLowerCase()).then(res => res.data);
  };

  getTableProcessStatus = (table: string) => {
    const params = {
      table: table.toLowerCase(),
    };
    return this.get(config.doe.doeService + 'process/getProcess', { params }).then(res => res.data);
  };

  getUpstreamHistory = (table: string, platform: 'hercules' | 'apollo_rno' | 'numozart') => {
    const params = {
      table: table.toLowerCase(),
      platform,
    };
    return this.get(config.doe.doeService + 'process/getUpstreamHist', { params }).then(res => res.data.value);
  };

  getLatency = (table: string) => {
    return this.get(config.doe.url + 'operation/getLatency?table=' + table.toLowerCase()).then(res => res.data);
  };

  getFrequency = (table: string) => {
    return this.get(config.doe.url + 'operation/getFrequency?table=' + table.toLowerCase()).then(res => res.data);
  };

  queryTable = (table: string, size?: any) => {
    return this.get(config.doe.doeService + 'dependency/getTableList?table=' + table + (size ? '&size=' + size : '')).then(res => res.data.value);
  };

  getMyCollectionList = (nt?: string) => {
    return this.get(config.doe.url + 'mycollection/getMyCollectionList?nt=' + (nt ? nt : Util.getNt())).then(res => res.data);
  };

  insertMyCollectionList = (params: any) => {
    return this.post(config.doe.url + 'mycollection/insertMyCollectionList', params);
  };

  deleteMyCollectionList = (params: any) => {
    return this.post(config.doe.url + 'mycollection/deleteMyCollectionList', params);
  };

  getCollectionFinshTimeLine = (platform: string, params: any) => {
    return this.post(config.doe.doeService + 'process/getFinishTimeHist?platforms=' + platform, params);
  };

  getReconFailureRate = (platform: string, params: any) => {
    return this.post(config.doe.doeService + 'process/getReconFailureRate?platforms=' + platform, params);
  };

  getUsagePersona = (table: string, depth: number,) => {
    return this.get(config.doe.doeService + `process/getUsagePersona?table=${table}&depth=${depth}`).then(res => res.data);
  };
  getUsagePersonaSummary = (table: string, depth: number,) => {
    return this.get(config.doe.doeService + `process/getUsagePersonaSummary?table=${table}&depth=${depth}`).then(res => res.data.data);
  };
  registerDonefile = (params: { db: string; platforms: string; table: string; type: string; user: string; donefile: string }[]) => {
    return this.post(config.doe.doeService + 'dependency/registerDonefile', params);
  };
  getInformationLayerOptions = () => {
    return this.get(config.doe.doeService + 'data_model/getInformationLayerOptions').then(res => res.data.data);
  };
  getSlackUserId = (nt: string) => {
    return super.get(`https://slack.com/api/auth.findUser?team=T0M05TDH6&email=${nt}@ebay.com`, undefined, undefined, true).then(res => res.data.user_id);
  };
  getTbMngrTab = () => {
    return this.get(config.doe.doeService + 'table_management/getTab').then(res => res.data.data);
  };
  getLabelsAuthority = (nt: string, tableName: string) => {
    return this.get(config.doe.doeService + 'metadata/getLabelsAuthority?nt=' + nt + '&tableName=' + tableName).then(res => res.data.data);
  };
}
