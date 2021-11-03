import { Tags } from '../tag';
import Metric from './metric';
import _ from 'lodash';
import Util from '@/services/Util.service';
import config from '@/config/config';
function evnMapper(env: string) {
  let trackingEnv: 'qa' | 'prod' = 'qa';
  switch (env) {
    case 'development':
    case 'staging':
      trackingEnv = 'qa';
      break;
    case 'production':
      trackingEnv = 'prod';
      break;
  }
  return trackingEnv;
}
function generateTags() {
  const app = 'ZDS_WEB';
  const env =  evnMapper(config.env || '').toUpperCase();
  return {
    userAgent: navigator.userAgent,
    // eslint-disable-next-line @typescript-eslint/camelcase
    nt_login: Util.getNt(),
    host: location.hostname,
    app,
    env
  };
}
export default class MetricsPacket {
  private commonTags: Tags;
  private metrics: Metric[];
  constructor() {
    this.commonTags = generateTags();
    this.metrics = [];
  }
  public push(metric: Metric) {
    this.metrics.push(metric);
  }
  public serialization(): string {
    return JSON.stringify(_.pick(this, ['commonTags', 'metrics']));
  }
  public clear() {
    this.metrics = [];
  }
}