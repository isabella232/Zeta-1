import { Vue, Component, Provide } from 'vue-property-decorator';
import { State } from 'vuex-class';
import { IFile } from '@/types/repository';
import _ from 'lodash';

@Component
export default class FolderOptionsProvider extends Vue {

  @State(state => state.repository.files)
  fileDict: Dict<IFile>;
  
  @Provide()
  getfolderOptions() {
    return _.chain(this.fileDict)
      .map(file => file.path)
      .map(path => {
        if (!_.startsWith(path, '/')) {
          path = '/' + path;
        }
        if (!_.endsWith(path, '/')) {
          path = path + '/';
        }
        return path;
      })
      .uniq()
      .value();
  }  
}
