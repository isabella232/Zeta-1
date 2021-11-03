import { Tags } from '../tag';
import moment from 'moment';
export type MetricType  = 'gauge' | 'timer' | 'counter';

const METRIC_PREFIX = 'ZDS_WEB_';
export default class Metric {
  private tags: Tags;
  private type: MetricType;
  private name: string;

  constructor(name: string, type: MetricType) {
    this.name = METRIC_PREFIX + name;
    this.type = type;
    //! do not assign a timestamp like params in tag
    const date = moment().utc();
    this.tags = {
      year: date.format('YYYY'),
      month: date.format('MM'),
      weekOfYear: date.format('WW'),
    };
  }

  public setTags(tags: Tags) {
    this.tags = {
      ...this.tags,
      ...tags
    };
    return this;
  }
}