import { scheduleConfigFactory, NotebookSchedule } from '@/components/common/schedule-container';
import { CodeType } from '@/types/workspace';

import { shallowMount } from '@vue/test-utils';
import { expect } from 'chai';
import { spy, stub } from 'sinon';
import Util from '@/services/Util.service';
import _ from 'lodash';
const notebookId = 'UnitTest';
describe('schedule config factory', () => {
  before(() => {
    stub(Util, 'getNt').returns('tester');
  });
  it('spark sql => ares',() => {
    const clusterAlias = 'Ares';
    const clusterId = 2;
    const result: NotebookSchedule = scheduleConfigFactory(CodeType.SQL,notebookId, clusterId, clusterAlias);
    const config: NotebookSchedule = {
      clusterId: 2,
      proxyUser: 'tester',
      req: {
        notebookId: 'UnitTest',
        interpreter: 'livy-sparksql',
        codes: null,
        reqId: null
      }
    };
    expect(result).to.deep.equal(config);
  });
  it('spark sql => hermes',() => {
    const clusterAlias = 'Hermes';
    const clusterId = 16;
    const result: NotebookSchedule = scheduleConfigFactory(CodeType.SQL,notebookId, clusterId, clusterAlias);
    const config: NotebookSchedule = {
      clusterId: 16,
      proxyUser: 'tester',
      req: {
        notebookId: 'UnitTest',
        interpreter: 'carmel',
        codes: null,
        reqId: null
      },
      prop: {
        host: 'hermes.prod.vip.ebay.com',
        user: 'tester',
        password: '',
        database: 'access_views',
        'jdbc.props.hive.server2.remote.principal': 'b_carmel/hermes.prod.vip.ebay.com@PROD.EBAY.COM',
        port: 10000,
        jdbc_type: 'carmel',
      }
    };
    expect(result).to.deep.equal(config);
  });
  it('td sql', () => {
    const result: NotebookSchedule = scheduleConfigFactory(CodeType.SQL,notebookId, -1, 'Mozart');
    const config: NotebookSchedule = {
      proxyUser: 'tester',
      req: {
        notebookId: 'UnitTest',
        interpreter: 'jdbc',
        codes: null,
        reqId: null
      },
      prop: {
        host: 'mozart.vip.ebay.com',
        user: 'tester',
        password: '',
        database: 'default',
        'jdbc.props.hive.server2.remote.principal': '',
        port: 10000,
        jdbc_type: 'jdbc',
      }
    };
  });
  it('hive sql', () => {
    const result: NotebookSchedule = scheduleConfigFactory(CodeType.SQL,notebookId, -1, 'Hercules');
    const config: NotebookSchedule = {
      proxyUser: 'tester',
      req: {
        notebookId: 'UnitTest',
        interpreter: 'jdbc',
        codes: null,
        reqId: null
      },
      prop: {
        host: 'hercules-lvs-rm-1.vip.ebay.com',
        user: 'tester',
        password: '',
        database: 'default',
        'jdbc.props.hive.server2.remote.principal': 'hive/hercules-lvs-rm-1.vip.ebay.com@APD.EBAY.COM',
        port: 10000,
        jdbc_type: 'hive',
      }
    };
  });
});