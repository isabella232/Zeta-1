import BaseRemoteService from '@/services/remote/BaseRemoteService';
import * as Axios from 'axios';
import Util from '@/services/Util.service';
import Config from '@/config/config';
export default abstract class DssAPI extends BaseRemoteService {
  protected generateConfig (config?: Axios.AxiosRequestConfig | undefined, data?: any, original = false): Axios.AxiosRequestConfig {
    const defaultConfig = super.generateConfig(config, data);
    if (!original) {
      // (defaultConfig.headers as any)['Authorization'] = 'Bearer ' + window.dssAuthClient.getToken();
      // (defaultConfig.headers as any)['nt'] = 'tianrsun';
      // (defaultConfig.headers as any)['Zeta-Authorization'] = 'Bearer ' + Util.getZetaToken();
      defaultConfig.headers = Util.getResuestHeader();
    }
    return defaultConfig;
  }
}
