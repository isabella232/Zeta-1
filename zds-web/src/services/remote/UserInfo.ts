import config from '@/config/config';
import DssAPI from '@/services/remote/dss-api-remote-service';
import Util from '../Util.service';
import * as Axios from 'axios';
const axios = Axios.default;

export default class UserInfoRemoteService extends DssAPI {

  static readonly instance = new UserInfoRemoteService();

  getBaseInfo (nt: string){
    const url = `${config.doe.url}solr/typeAhead?core=user&q=${nt}&field=nt&type=nt`;
    return this.get(url);
  }
  getUserPreference (){
    return this.get(`${config.zeta.base}users/me`);
  }
  setUserPreference (nt: string, name: string, preference: any){
    return this.put(`${config.zeta.base}users/me`, {
      nt:nt,
      name:name,
      tdPass: null,
      githubToken: null,
      windowsAutoAccountPass: null,
      preference:JSON.stringify(preference)
    });
  }
  setUserInfo(nt: string,name: string,tdPass: string | null, githubToken: string| null, winPass: string| null, preference?: any ){
    return this.put(`${config.zeta.base}users/me`,{
      nt:nt,
      name:name,
      tdPass:tdPass,
      githubToken:githubToken,
      windowsAutoAccountPass:winPass,
      preference: JSON.stringify(preference)
    });
  }
  kylinAuthInfo () {
    // config.headers.Authorization = "Bearer " + Util.getZetaToken()
    return axios.get(`//kylin.rno.corp.ebay.com/kylin/api/projects/readable/user/${Util.getNt()}`, {
      headers: {
        Authorization: 'Basic X05PVVNfUkVBRE9OTFk6MjAhN0BTb1NheVdlQWxsIQ=='
      }
    }).catch(() => {
      return { data: []};
    });
  }
  verifyToken () {
    return this.head(`${config.zeta.base}users/verifyToken`).catch(this.verifyTokenFailed);
  }
  private verifyTokenFailed (error: Axios.AxiosError) {
    if (error.response && error.response.status === 401) {
      Util.getApp().$store.dispatch('setTokenValid', false);
      Util.refreshPopup('Token expire', 'please refresh this page to get new auth token');
    }
    throw error;
  }
}
