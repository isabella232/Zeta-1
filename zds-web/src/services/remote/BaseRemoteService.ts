import * as Axios from 'axios';
import Util from '../Util.service';
import { ZetaException, ZetaExceptionProps, ZETA_EXCEPTION_TAG, isExceptionPacket, customZetaException } from '@/types/exception';
import _ from 'lodash';
const axios = Axios.default;
const isFormData = (data: any) => {
  return Object.prototype.toString.call(data) === '[object FormData]';
};

const cancelToken = (config: Axios.AxiosRequestConfig, cancel: () => void) => {
  new axios.CancelToken((c: Axios.Canceler) => {
    cancel = c;
  });
};
const isTokenExpired = (error: any) => {
  try {
    if (error && error.response && error.response.status === 401){
      const isBlob = error.response.config.responseType === 'blob';
      if (isBlob) return true;
      if (error.response.data.error === 'Zeta token authentication failed') return true;
    }
  } catch (e) {
    return false;
  }
  return false;
};
export default abstract class BaseRemoteService{
  protected generateConfig(config?: Axios.AxiosRequestConfig | undefined, data?: any, original = false): Axios.AxiosRequestConfig {
    const defaultConfig = {
      headers: {}
    };
    config = Object.assign(defaultConfig, config);
    // set data
    if (!_.isEmpty(data) || isFormData(data)) {
      config.data = data;
    }
    // // set token
    // if(!original) {
    //   config.headers.Authorization = 'Bearer ' + Util.getZetaToken();
    // }
    return config;
  }
  /**
     *
     * @param error
     * @param url
     * @param props
     *
     * $logger
     * exceptionstore
     */
  protected defaultErrorHandler (error: any, url: string, props: ZetaExceptionProps = { path : 'unknown'}): ZetaException {

    if (isTokenExpired(error))  {
      console.warn('zeta token error');
      /* Token expire.
             * For the most of time Zeta token expire.
             * Then clear all token and redirect to SSO.
             */
      Util.refreshPopup('Token expire', 'please refresh this page to get new auth token');
      Util.getApp().$store.dispatch('setTokenValid', false);
      const exception = new ZetaException(error.response.data || {}, props).tags([ZETA_EXCEPTION_TAG.REST_ERR]).resolve();
      Util.getApp().$store.dispatch('addException', {exception});
      return  exception;
    }
    else {
      console.error('REST ERR: ', error);
      let exception: ZetaException;
      if (!error.response) {
        exception = customZetaException('[No Response]REST ERROR: '+ url, {}, props).tags([ZETA_EXCEPTION_TAG.REST_ERR]);
      } else if (error.response.data && isExceptionPacket(error.response.data)) {
        exception = new ZetaException(error.response.data, props, error.response.headers).tags([ZETA_EXCEPTION_TAG.REST_ERR]);
      } else {
        exception = customZetaException('REST ERROR: '+ url, error.response, props).tags([ZETA_EXCEPTION_TAG.REST_ERR]);
      }
      Util.getApp().$store.dispatch('addException', {exception});
      return exception;
    }
  }
  protected properties: ZetaExceptionProps = {
    path: 'unknown'
  };
  private tags: ZETA_EXCEPTION_TAG[] = [];
  protected responseHandler (response: Axios.AxiosResponse): any{
    return response.data;
  }
  public props (props: ZetaExceptionProps) {
    // this.properties = props;
    Object.assign(this.properties, props);
    return this;
  }
  public tag (tag: ZETA_EXCEPTION_TAG) {
    this.tags.push(tag);
    return this;
  }
  public get <T = any> (url: string, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler, original = false): Axios.AxiosPromise<T>{
    config = this.generateConfig(config, undefined, original);
    if (canceler) cancelToken(config, canceler);
    return this.request(url, config, canceler);
  }
  public post <T = any> (url: string, data?: any, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler, original = false): Axios.AxiosPromise<T> {

    config = this.generateConfig(config, data, original);
    config.method = 'POST';
    if (canceler) cancelToken(config, canceler);
    return this.request(url, config, canceler);
  }
  public put <T = any> (url: string, data?: any, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler, original = false): Axios.AxiosPromise<T>  {
    config = this.generateConfig(config, data, original);
    config.method = 'PUT';
    if (canceler) cancelToken(config, canceler);
    return this.request(url, config, canceler);
  }
  public delete<T = any> (url: string, data?: any, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler, original = false): Axios.AxiosPromise<T> {
    config = this.generateConfig(config, data, original);
    config.method = 'DELETE';
    if (canceler) cancelToken(config, canceler);
    return this.request(url, config, canceler);
  }
  protected head (url: string) {
    const config = this.generateConfig();
    config.method = 'HEAD';
    return this.request(url, config);
  }
  protected request <T = any> (url: string, config?: Axios.AxiosRequestConfig | undefined, canceler?: Axios.Canceler): Axios.AxiosPromise<T> {
    const method = config && config.method ? config.method : 'GET';
    const data = config && config.data ? config.data : {};
    console.debug(`REST [${method}]: ` + url + ' [DATA]:' + (data ? JSON.stringify(data) : '{}'));
    const props = this.properties;
    return axios(url, config).then( res => res, (error: any) => {
      throw this.defaultErrorHandler(error, url, props);
    });
  }
}
