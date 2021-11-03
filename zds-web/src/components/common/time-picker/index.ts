import _ from 'lodash';
import moment from 'moment';
export const FrequenceOptions = [
  {value: 'ONETIME', name: ''},
  {value: 'HOURLY', name: 'HOUR'},
  {value: 'DAILY', name: 'DAY'},
  {value: 'WEEKLY', name: 'WEEK'},
  {value: 'MONTHLY', name: 'MONTH'}
];
export const DateOptions = _.chain(_.times(30))
  .map(t => t + 1 + '')
  .value();
export const MinuteOptions = _.chain(_.times(60))
  .map(t => t + '')
  .value();
export const DayOptions = [
  {
    key: 'SUN',
    value: '1'
  },
  {
    key: 'MON',
    value: '2'
  },
  {
    key: 'TUE',
    value: '3'
  },
  {
    key: 'WEN',
    value: '4'
  },
  {
    key: 'THU',
    value: '5'
  },
  {
    key: 'FRI',
    value: '6'
  },
  {
    key: 'SAT',
    value: '7'
  }
];
export type Frequence = 'ONETIME' | 'MONTHLY' | 'WEEKLY' | 'DAILY' | 'HOURLY';
export class TimePickerResult {
  jobType: Frequence;
  year?: number;
  dayOfMonths?: number;
  dayOfWeeks?: number;
  month?: number;
  hour?: number;
  minute?: number;
  constructor(){
    this.jobType = 'DAILY';
    const datetime = new Date(moment().utcOffset(-7).format('YYYY/MM/DD HH:mm:ss'));
    this.year = datetime.getFullYear();
    this.month = datetime.getMonth();
    this.dayOfMonths = datetime.getDate();
    this.dayOfWeeks = datetime.getDay();
    this.hour = datetime.getHours();
    this.minute = datetime.getMinutes();
  }
}
import TimePicker from './TimePicker.vue';
export default TimePicker;
