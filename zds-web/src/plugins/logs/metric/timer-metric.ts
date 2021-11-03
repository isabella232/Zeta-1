import Metric from './metric';

export default class TimerMetric extends Metric {
  private durationInMillis = 0;
  constructor(name: string, duration = 0) {
    super(name, 'timer');
    this.durationInMillis = duration;
  }
}