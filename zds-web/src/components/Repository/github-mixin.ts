import { Vue, Component } from 'vue-property-decorator';
import _ from 'lodash';
import { ZetaFile } from './github/types';
import { IFile } from '@/types/repository';
import { initRepository } from '@/services/workspace-init.service';

@Component
export default class GithubMixin extends Vue {
  readonly defaultLink = ''; //'https://github.corp.ebay.com/DSS-CCOE/zeta-dev-suite'
  onPullSuccess(){
    initRepository(this).then(() => {
      this.$message({message:'pull from github success !', type: 'success'});
    });
  }
  // eslint-disable-next-line @typescript-eslint/no-empty-function,@typescript-eslint/no-unused-vars
  onPullFail(msg: string){
  }
  getFilesPath(files: IFile[]): ZetaFile[]{
    return _.map(files, (file: IFile) => {
      const path = file.path[file.path.length -1] == '/' ? file.path : file.path + '/';
      return {
        fullPath: file.path === '/' ? file.title : path + file.title,
        title: file.title,
        notebookId: file.notebookId,
        selected: false,
      } as ZetaFile;
    });
  }

  onPushSuccess(){
    this.$message({message:'push to github success !', type: 'success'});
  }
  // eslint-disable-next-line @typescript-eslint/no-empty-function,@typescript-eslint/no-unused-vars
  onPushFail(msg: string){
  }
}
