import { AxiosError } from "axios";
import _ from "lodash";
function chainFunc(arg: any){
    return {
        then: (fnc: (a: any) => void) => fnc(arg)
    }
}
export const ERROR_MAP = {
    'NO_TOKKEN_FOUND': 'please reset your Github token',
    'OTHERS': 'error occurred'
}
export function errprHandler(error: AxiosError){
    if(error.response && error.response.data){
        const data = error.response.data
        if(data && data.status === '008'){
            return chainFunc(data.message)
        }
    }
    console.warn('cannot resolve error',error)
    return chainFunc(ERROR_MAP.OTHERS);
}
export function getStorageSearchUrl(key: string){
    const json = localStorage.getItem(key) || '';
    if (!json) {
        return [];    
    }
    let url = JSON.parse(json);
    return url && url.length>0 ? url : [] ;
}
export function saveStorageSearchUrl(key: string,url: string){
    let exist_url = getStorageSearchUrl(key);
    exist_url.push({value: url});
    localStorage.setItem(key, JSON.stringify(_.uniqBy(exist_url,'value')));
}
export function createFilter(queryString: string) {
    return (url: any) => {
        return (url.value.toLowerCase().indexOf(queryString.toLowerCase()) > -1);
    };
}