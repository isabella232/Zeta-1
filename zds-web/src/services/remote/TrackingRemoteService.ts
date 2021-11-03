import axios from 'axios';
import config from '@/config/config';

class TrackingRemoteService {
    url: string = '';
    constructor() {
        this.url = `${config.tracking.url}` || '';
    }
    metrics(data: any) {
        const url = this.url + '/api/v1/metrics'
        return axios.post(url, data)
    }
}
export const trackingRemoteService = new TrackingRemoteService();