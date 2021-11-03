import config from "@/config/config";
import * as Axios from 'axios';
import BaseRemoteService from "@/services/remote/BaseRemoteService";
export default class ESBaseRemoteService extends BaseRemoteService{
    responseHandler(response:Axios.AxiosResponse):any{
        return response.data
    }
    parseResult(data:any):any{
        if(data.hits && data.hits.hits){
            return data.hits.hits.map((r:any) => r._source)
        }
        else{
            return []
        }
    }
}