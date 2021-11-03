import { Vue, Component, Provide } from 'vue-property-decorator';
import { NoteType } from './create-notebook-form/note-options';
import { State } from 'vuex-class';
import { Cluster } from '@/types/workspace';

@Component
export default class PlatformPermissionProvider extends Vue {

  @State(state => state.user.user.clusterOption['ApolloReno'])
  reno: Cluster;

  @State(state => state.user.user.clusterOption['Hermes'])
  hermes: Cluster;

  @State(state => state.user.user.kylinProjects)
  kylinProjects: string[];

  @Provide()
  isDisabledPlatform(platform: string, noteType: NoteType) {
    if (noteType === 'solo') {
      return false;
    }
    if (platform === 'kylin' && noteType === 'stacked') {
      return !this.kylinProjects || this.kylinProjects.length == 0;
    }

    if (platform === 'python') {
      return false;
    }

    if (platform === 'apollo') {
      return !this.reno.access;
    }
    if (platform === 'hermes') {
      return !this.hermes.access;
    }
    return false;
  }
}
