import Metric from './metric';

export default class CounterMetric extends Metric {
  private increment: number;
  constructor(name: string, increment = 1) {
    super(name, 'counter');
    this.increment = increment;
  }
}