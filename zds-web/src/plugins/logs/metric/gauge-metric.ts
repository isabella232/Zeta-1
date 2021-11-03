import Metric from './metric';

export default class GaugeMetric extends Metric {
  private value: number;
  constructor(name: string, value = 0) {
    super(name, 'gauge');
    this.value = value;
  }
}