import { Vue, Component } from 'vue-property-decorator';
import { Getter, Action, State } from 'vuex-class';
import { IFile } from '@/types/workspace';
import { ScheduleConfig } from '@/components/common/schedule-container';
import _ from 'lodash';
const PREFERENCE_KEY = 'ignoreAlationAlert_V1';
const PREFERENCE_SCHEDULE_KEY = 'ignoreAlationAlert_V2';
const PREFERENCE_ALATION_SYNC_KEY = 'alation_sync_date';
const LAST_UPDATE_DT = '2020/03/07';
@Component
export default class AlationAlertMixin extends Vue {
  @Getter('userPreferenceByKey') userPreference: (key: string) => any
  @Action updateUserPreference: (preference: Dict<any>) => Promise<any>

  @State(state => state.repository.files)
  allfiles: Dict<IFile>

  @State(state => state.ScheduleStore.taskList)
  alltaskList: Array<ScheduleConfig>

  get hasAlationNotes() {
    return _.find(this.allfiles, file => file.path === '/Alation_Migration/');
  }
  get hasAlationTasks() {
    return _.find(this.alltaskList, task => _.toLower(task.jobName).indexOf("[alation synced]") == 0);
  }
  get showAlationAlert() {
    const alert = !this.userPreference(PREFERENCE_KEY);
    return alert && this.hasAlationNotes;
  }
  get showScheduleAlationAler() {
    const alert = !this.userPreference(PREFERENCE_SCHEDULE_KEY);
    return alert && this.hasAlationTasks;
  }
  get alationAlertContent() {
    const alationSyncDate = this.userPreference(PREFERENCE_ALATION_SYNC_KEY);
    const alationAlertContent = `All of your Alation scripts updated before ${alationSyncDate ? alationSyncDate : LAST_UPDATE_DT} are availible on Zeta Now. Check it at folder /Alation_Migration/`;
    return alationAlertContent;
  }
  get scheduleAlationAlertContent() {
    const alationSyncDate = this.userPreference(PREFERENCE_ALATION_SYNC_KEY);
    const scheduleAlationAlertContent = `All of your Alation scheduled job edited before ${alationSyncDate ? alationSyncDate : LAST_UPDATE_DT} are availible on Zeta Now. Check the job name start with [Alation synced]`;
    return scheduleAlationAlertContent;
  }
  ignoreAlationAlert() {
    this.updateUserPreference({
      [PREFERENCE_KEY]: 1
    });
  }
  ignoreScheduleAlationAlert() {
    this.updateUserPreference({
      [PREFERENCE_SCHEDULE_KEY]: 1
    });
  }
}
