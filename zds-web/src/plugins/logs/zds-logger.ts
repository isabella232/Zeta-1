import PacketMannger from './packet-manager';
import { Tags } from './tag';
import CounterMetric from './metric/counter-metric';
import GaugeMetric from './metric/gauge-metric';
import TimerMetric from './metric/timer-metric';

const packetManager = PacketMannger.getInstance();
export function counter(name: string, value?: number, tags?: Tags) {
  const m = new CounterMetric(name, value);
  if (tags) {
    m.setTags(tags);
  }
  packetManager.push(m);
}
export function gauge(name: string, value?: number, tags?: Tags) {
  const m = new GaugeMetric(name, value);
  if (tags) {
    m.setTags(tags);
  }
  packetManager.push(m);
}

export function timer(name: string, duration: number, tags?: Tags) {
  const m = new TimerMetric(name, duration);
  if (tags) {
    m.setTags(tags);
  }
  packetManager.push(m);
}