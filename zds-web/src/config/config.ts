import { IConnection, CodeType } from '@/types/workspace';
const PREFIX = process.env.VUE_APP_ZETA_DOMIAN_PREFIX || '';
const env = process.env.VUE_APP_ENV;
const domain =process.env.VUE_APP_ZETA_DOMIAN || `//${location.hostname}/`;
const wsDomain = process.env.VUE_APP_ZETA_WS_DOMIAN || domain;
const root: string = domain + PREFIX as string;
export default {
  env,
  // enableSSO: eval(process.env.VUE_APP_ENABLE_OSS || 'true'),
  webOSSOn: process.env.WEB_OSS_ON,
  excludeSSO: ['/error'],
  zeta: {
    esServer: process.env.VUE_APP_ZETA_ES || domain,
    domain: domain,
    base: domain + PREFIX,
    wsDomain,
    bdp: 'https://bdp.vip.ebay.com/product/user/',
    auth: domain + PREFIX + 'login',
    google: process.env.VUE_APP_ZETA_GOOGLE_DOMIAN,
    notification: process.env.VUE_APP_ZETA_NOTIFICATION_DOMIAN,
    nodesvc: process.env.VUE_APP_ZETA_NODESVC_DOMIAN,
    notebook: {
      endpoints: {
        connect: wsDomain + PREFIX + 'ws/',
        nbConnect: '/wsapp/code/connect',
        nbDisconnect: '/wsapp/code/disconnect',
        response: '/user/queue/square',
        execute: '/wsapp/code/execute',
        dump: '/wsapp/code/dump',
        cancel: '/wsapp/code/cancel',
        reqId: '/wsapp/id',
        recover: 'nbConnect',
      },
      optimization: {
        defaultPrflName: 'default',
      },
      connection: {
        //clusters: ['areslvs', 'apollophx', 'apollorno', 'herculeslvs', 'herculessublvs','hermesrno','herculesrno'],
        // clusters: ['areslvs', 'apollophx', 'apollorno', 'herculeslvs', 'herculessublvs','hermesrno'],
        clusters: ['areslvs', 'apollorno', 'herculeslvs', 'hermesrno', 'hermeslvs'],
        clustersMap: {
          'areslvs': 'Ares',
          // 'apollophx': 'Apollo',
          'apollorno': 'ApolloReno',
          'herculeslvs': 'Hercules',
          // 'herculessublvs': 'HerculesSub',
          'hermesrno': 'Hermes',
          'hermeslvs': 'HermesLVS',
          // 'herculesrno': 'HerculesRno'
        } as Dict<string>,
        clustersIdMap: {
          'areslvs': 2,
          // 'apollophx': 3,
          'apollorno': 14,
          'herculeslvs': 10,
          // 'herculessublvs': 11,
          'hermesrno': 16,
          'hermeslvs': 21,
        } as Dict<number>,
        hiveOrTdClustersAliasIdMap: {
          'Ares': 2,
          'ApolloReno': 14,
          'Hercules': 10,
          // 'Hopper': 90,
          'Mozart': 91,
        } as Dict<number>,
        allAliasIdMap: {
          'Ares': 2,
          'ApolloReno': 14,
          'Hercules': 10,
          'Hermes': 16,
          'HermesLVS': 21,
          // 'Hopper': 90,
          'Mozart': 91,
        } as Dict<number>,
        get defaultConnection () {
          return  {
            alias: 'Hermes',
            source: 'hermesrno',
            clusterId: 16,
            batchAccount: '',
            codeType: CodeType.SQL,
          } as IConnection;
        },
        jdbcConfig: {
          16: {
            host: 'hermes.prod.vip.ebay.com',
            interpreter: 'carmel',
            jdbcType: 'carmel',
            pricipal: 'b_carmel/hermes.prod.vip.ebay.com@PROD.EBAY.COM',
          },
          21: {
            host: 'hermes-lvs.prod.vip.ebay.com',
            interpreter: 'carmel',
            jdbcType: 'carmel',
            pricipal: 'b_carmel/hermes-lvs.prod.vip.ebay.com@PROD.EBAY.COM',
          },
        },
      },
      pyConnection: {
        clusters: ['areslvs', 'apollorno', 'herculeslvs'],
        // clusters:['areslvs','apollophx','apollorno','herculeslvs','herculessublvs'],
        // clusters:['areslvs','apollophx','herculeslvs'],
        clustersMap: {
          areslvs: 'Ares',
          // 'apollophx':'Apollo',
          apollorno: 'ApolloReno',
          herculeslvs: 'Hercules',
          // 'herculessublvs':'HerculesSub'
        } as Dict<string>,
        defaultConnection: {
          alias: 'ApolloReno',
          source: 'apollorno',
          clusterId: 14,
          batchAccount: '',
          codeType: CodeType.SPARK_PYTHON,
        } as IConnection,
      },
      tdConnection: {
        clustersMap: {
          numozart: 'Mozart',
          // 'mozart':'Mozart',
          // 'hopper':'Hopper'
        } as Dict<string>,
        hostMap: {
          // 'Mozart': 'mozart.vip.ebay.com',
          // 'Hopper': 'hopper.vip.ebay.com',
          'Mozart': 'Mozart.lvs.ebay.com',
        } as Dict<string>,
        get defaultConnection () {
          return {
            alias: 'Mozart',
            source: 'Mozart.lvs.ebay.com',
            clusterId: -1,
            batchAccount: '',
            codeType: CodeType.TERADATA,
          } as IConnection;
        },
      },
      kylinConnection: {
        clustersMap: {
          // 'Kylin': 'Kylin',
          // 'Kylin-qa': 'Kylin-qa',
          'Kylin-rno': 'Kylin-rno',
          'Kylin-rno-qa': 'Kylin-rno-qa',
        } as Dict<string>,
        hostMap: {
          // 'Kylin': 'kylin.corp.ebay.com',
          // 'Kylin-qa': 'kylin-qa.corp.ebay.com',
          'Kylin-rno': 'kylin.rno.corp.ebay.com',
          'Kylin-rno-qa': 'kylin-qa.rno.corp.ebay.com',
        } as Dict<string>,
        get defaultConnection () {
          return {
            alias: 'Kylin-rno',
            source: 'kylin.rno.corp.ebay.com',
            clusterId: -1,
            batchAccount: '',
            codeType: CodeType.KYLIN,
            user: '',
            password: '',
            database: '',
          } as IConnection;
        },
      },
      hiveConnection: {
        clustersMap: {
          areslvs: 'Ares',
          // apollophx: 'Apollo',
          herculeslvs: 'Hercules',
          // herculesrno: 'HerculesRno'
          apollorno: 'ApolloReno',
        } as Dict<string>,
        hostMap: {
          // HerculesRno: 'rnohdc43en0005.rno.ebay.com',
          ApolloReno: 'rnohdc42en0005.rno.ebay.com',
          Hercules: 'hercules-lvs-rm-1.vip.ebay.com',
          Ares: 'ares-hv.vip.ebay.com',
          // Apollo: 'apollo-phx-oz.vip.ebay.com'
        } as Dict<string>,
        principalMap: {
          // HerculesRno: 'hive/rnohdc43en0005.rno.ebay.com@PROD.EBAY.COM',
          ApolloReno: 'hive/rnohdc42en0005.rno.ebay.com@PROD.EBAY.COM',
          Hercules: 'hive/hercules-lvs-rm-1.vip.ebay.com@APD.EBAY.COM',
          Ares: 'hadoop/lvsaishdc3hn0002-be.lvs.ebay.com@APD.EBAY.COM',
          // Apollo: 'hadoop/apollo-phx-oz.vip.ebay.com@APD.EBAY.COM'
        } as Dict<string>,
        get defaultConnection () {
          return {
            alias: 'Hercules',
            source: 'hercules-lvs-rm-1.vip.ebay.com',
            clusterId: -1,
            batchAccount: '',
            codeType: CodeType.HIVE,
            user: '',
            password: '',
            database: '',
          } as IConnection;
        },
      },
    },
    repository: {
      root,
    },
    zeppelin: {
      domain: process.env.VUE_APP_ZEPPELIN_DOMIAN,
      api: process.env.VUE_APP_ZEPPELIN_API || process.env.VUE_APP_ZEPPELIN_DOMIAN,
      ws: process.env.VUE_APP_ZEPPELIN_WS || (process.env.VUE_APP_ZEPPELIN_DOMIAN + 'ws'),
    },
  },
  doe: {
    url: process.env.VUE_APP_DOE_SERVICE_HOST ? `https://${process.env.VUE_APP_DOE_SERVICE_HOST }/api/v0/` : 'http://opsins-service.dss-doe.svc.57.tess.io/',
    doeService: process.env.VUE_APP_DOE_SERVICE_HOST ? `https://${process.env.VUE_APP_DOE_SERVICE_HOST}/api/v1/` : 'http://doe-spring-qa-service.dss-doe.svc.57.tess.io/',
  },
  oss: {
    majorurl: '//ossserver6.dss.vip.ebay.com/userinfo/major/',
    avatarUrl: '//ossserver6.dss.vip.ebay.com/userinfo/icon/',
  },
  nt: '',
  githubUrl: 'https://github.corp.ebay.com/APD/DINT-CORE/blob/master/etl/sql/',
  mockHistory: false,
  enableLog: eval(process.env.VUE_APP_ENABLE_LOG || 'false'),
  tracking: {
    enable: Boolean(process.env.VUE_APP_TRACKING),
    url: process.env.VUE_APP_TRACKING,
  },
};
