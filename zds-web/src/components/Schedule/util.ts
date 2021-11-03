import moment from 'moment';
import _ from 'lodash';
import { ScheduleType, ScheduleConfig, ScheduleHistory, ScheduleJobStatus, NoteScheduleDependency, NoteScheduleDependencySignal, DoeDependencySignal } from '@/components/common/schedule-container';
import { TimePickerResult } from '@/components/common/time-picker';
import Util from '@/services/Util.service';

export default class ScheduleUtil {

  static jobStatus (row: ScheduleHistory|ScheduleConfig){
    if (!row.jobRunStatusInfo || !row.jobRunStatusInfo.jobRunStatus)
      return '';
    return ScheduleJobStatus[row.jobRunStatusInfo.jobRunStatus];
  }
  static formatDateFromTimestamps (timestamps: number) {
    return timestamps
      ? moment(timestamps)
        .utcOffset('-07:00')
        .format('YYYY/MM/DD HH:mm:ss')
      : '';
  }

  static formartDuration (row: ScheduleHistory){
    if (!row.endTime){
      return '';
    }
    const duration = moment.duration(row.endTime - row.runTime);
    if (duration.asHours() >= 1) {
      return `${duration.hours()}h ${duration.minutes()}m ${duration.seconds()}s`;
    }
    if (duration.asMinutes() >= 1) {
      return `${duration.minutes()}m ${duration.seconds()}s`;
    }
    if (duration.asSeconds() >= 1) {
      return `${duration.seconds()}s`;
    }
    return '0s';
  }

  static parseTask (task: any) {
    if (task && task.history && (task.history.type == 4 || task.history.type == 5)) {
      const sourceTable = JSON.parse(task.history.sourceTable);
      task.history.sourceTable = _.isArray(sourceTable ) ? sourceTable .join(';') : sourceTable;
    }
    return task;
  }
  static getDefaultConfig (type: ScheduleType, task?: ScheduleConfig['task']): ScheduleConfig {
    const _task = task ? task:{} as ScheduleConfig['task'];
    return {
      jobName: '',
      status: 1,
      type,
      scheduleTime: new TimePickerResult(),
      nt: Util.getNt(),
      task: _task,
      mailSwitch: 1,
      ccAddr: null,
      failTimesToBlock: 0,
      autoRetry: false,
      dependency: {
        enabled: false,
        dependencyTables: [],
        waitingHrs: 1,
      } as NoteScheduleDependency,
      dependencySignal: {
        enabled: false,
        signalTables: [],
      } as NoteScheduleDependencySignal,
    } as ScheduleConfig;
  }

  static parseConfig (item){
    const scheduleTime = new TimePickerResult();
    const time = JSON.parse(item.scheduleTime);
    const dependency = {
      enabled: false,
      dependencyTables: [],
      waitingHrs: 1,
    };
    const dependencySignal = {
      enabled: false,
      signalTables: [],
    };
    const new_task = ScheduleUtil.parseTask(JSON.parse(item.task));
    scheduleTime.jobType = time.jobType;
    scheduleTime.dayOfMonths = time.dayOfMonths;
    scheduleTime.dayOfWeeks = time.dayOfWeeks;
    scheduleTime.hour = time.hour;
    scheduleTime.minute = time.minute;
    scheduleTime.month = time.month;
    const task: ScheduleConfig = {
      id: item.id,
      jobName: item.jobName,
      status: item.status,
      jobRunStatusInfo: item.jobRunStatusInfo,
      jobRunStatus: item.jobRunStatus,
      scheduleTime,
      type: item.type,
      task: new_task,
      nt: item.nt,
      createTime: item.createTime,
      lastRunTime: item.lastRunTime,
      nextRunTime: item.nextRunTime,
      updateTime: item.updateTime,
      ccAddr: JSON.parse(item.ccAddr),
      dependency: item.dependency ? JSON.parse(item.dependency) : dependency,
      dependencySignal: item.dependencySignal ? JSON.parse(item.dependencySignal) : dependencySignal,
      mailSwitch: item.mailSwitch,
      failTimesToBlock: item.failTimesToBlock?item.failTimesToBlock:0,
      autoRetry: item.autoRetry ? item.autoRetry : false,
      authInfo: JSON.parse(item.authInfo),
    };
    return task;
  }

  static  parseDoeDependencySignal (platform: string, tables: Array<string>){
    const doeDps = tables.map((name: string )=>{
      return {
        pltfrm_name: platform,
        db_name: name.split('.')[0],
        table_name: name.split('.')[1],
        scheduling: true,
      } as DoeDependencySignal;
    });
    return doeDps;
  }
}
