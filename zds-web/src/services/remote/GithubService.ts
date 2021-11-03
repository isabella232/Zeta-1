import config from '@/config/config';
import DssAPI from '@/services/remote/dss-api-remote-service';
import * as Axios from 'axios';
export default class GithubService extends DssAPI {
  fetchBranches (nt: string, url: string){
    return this.post(`${config.zeta.base}Git/getBranches`, {nt, url});
  }
  fetchFiles (nt: string, url: string, branch: string, folderSha?: string){
    return this.post(`${config.zeta.base}Git/getGitFiles`, {nt, url, branch, folderSha});
  }

  searchFiles (nt: string, url: string, branch: string, searchKeyword: string){
    return this.post(`${config.zeta.base}Git/searchFiles`, {nt, url, branch, searchKeyword});
  }
  checkNotebookExists (nt: string, url: string, branch: string, wsPath: string, fileList: any[]){
    return this.post(`${config.zeta.base}Git/checkNotebookExists`, {nt, url, branch, wsPath, fileList});
  }
  pull (nt: string, url: string, branch: string, wsPath: string, fileList: any[]){
    return this.post(`${config.zeta.base}Git/pull`, {nt, url, branch, wsPath, fileList});
  }

  createBranch (nt: string, url: string, branch: string, newBranch: string){
    return this.post(`${config.zeta.base}Git/createBranch`, {
      nt, url, branch, newBranch
    });
  }
  checkGitFileExists (nt: string, url: string, branch: string, fileList: any[]){
    return this.post(`${config.zeta.base}Git/checkGitFileExists`, {
      nt, url, branch, fileList
    });
  }
  push (nt: string, url: string, commitMessage: string, fileList: any[], branch?: string, newBranch?: string, tag?: string, requestConfig?: Axios.AxiosRequestConfig){
    let params: any = {
      nt, url, branch, commitMessage, fileList
    };
    if (branch) {
      params = {
        ...params,
        branch
      };
    }
    if (newBranch) {
      params = {
        ...params,
        newBranch
      };
    }
    if (tag) {
      params = {
        ...params,
        tag
      };
    }
    return this.post(`${config.zeta.base}Git/push`, params, requestConfig);
  }
}
