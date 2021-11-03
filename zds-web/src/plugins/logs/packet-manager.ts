import MetricsPacket from './metric/metrics-packet';
import Metric from './metric/metric';
import { trackingRemoteService } from '@/services/remote/TrackingRemoteService';
export default class PacketMannger {
  private static manager: PacketMannger;
  private timer: NodeJS.Timer;
  private packet: MetricsPacket;
  private constructor() {
    this.packet = new MetricsPacket();
    this.clearUnsendMetrics();
    this.startTimer();
  }
  public static getInstance() {
    if (this.manager) {
      return this.manager;
    } else {
      this.manager = new PacketMannger();
      return this.manager;
    }
  }
    
  public push(metric: Metric) {
    this.packet.push(metric);
    this.saveToLocal();
  }
  private clear() {
    this.packet.clear();
  }
  private saveToLocal() {
    localStorage.setItem('unsendMetrics', this.packet.serialization());
  }
  private sendMetrics(packet: MetricsPacket) {
    console.debug('====sendMetrics====', packet);
    return trackingRemoteService.metrics(packet);
  }
  private clearUnsendMetrics() {
    let packet: MetricsPacket | undefined = undefined;
    try {
      const json = localStorage.getItem('unsendMetrics') || '';
      if (!json) {
        localStorage.removeItem('unsendMetrics');
        return;    
      }
      packet = JSON.parse(json);
    } catch (e) {
      console.error('Error when parse local metrics', e);
    }
    if (packet) {
      this.sendMetrics(packet).then(() => {
        this.clear();
        localStorage.removeItem('unsendMetrics');
      });
    }
  }
  private startTimer() {
    this.timer = setInterval(() => {
      this.clearUnsendMetrics();
    }, 5 * 60 * 1000);
  }
  public terminate() {
    clearInterval(this.timer);
  }
}