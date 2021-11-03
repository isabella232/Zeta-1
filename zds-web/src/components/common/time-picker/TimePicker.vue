<template>
  <div class="zeta-time-picker">
    <div class="row">
      <div class="row-name">
        Repeat Mode
      </div>
      <div class="row-info">
        <el-switch
          :value="repeat"
          :disabled="isShared"
          @change="(val) => valueChangeHandler('repeat',val)"
        />
      </div>
    </div>
    <div class="row">
      <template v-if="!repeat">
        <div class="row-name">
          Set Time
        </div>
        <div class="row-info">
          <el-date-picker
            :value="datetime"
            :size="size"
            :editable="true"
            :clearable="false"
            :time-arrow-control="true"
            placeholder="Select date and time"
            type="datetime"
            format="yyyy-MM-dd HH:mm"
            :disabled="isShared"
            @input="(val) => valueChangeHandler('datetime',val)"
          />
          <span class="timezone">MST</span>
        </div>
      </template>
      <template v-else>
        <div class="row-name">
          Set Time
        </div>
        <div class="row-info">
          <el-select
            v-model="frequence"
            placeholder="Frequence"
            :size="cmpntSize"
            :disabled="isShared"
          >
            <el-option
              v-for="(item,$index) in frequenceOption"
              :key="$index"
              :label="item.name"
              :value="item.value"
            />
          </el-select>
          <!-- <template v-if="frequence == 'ONETIME'">
            <el-date-picker
              :value="datetime"
              :size="cmpntSize"
              :editable="true"
              :clearable="false"
              :time-arrow-control="true"
              placeholder="Select date and time"
              format="yyyy-MM-dd HH:mm"
              type="datetime"
              :disabled="isShared"
              @input="(val) => valueChangeHandler('datetime',val)"
            />
          </template> -->
          <template v-if="frequence == 'MONTHLY'">
            <span>on the</span>
            <el-select
              :value="date"
              :size="cmpntSize"
              placeholder="Date..."
              :disabled="isShared"
              @input="(val) => valueChangeHandler('date',val)"
            >
              <el-option
                v-for="item in dateOptions"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
            <span>at</span>
            <el-time-picker
              arrow-control
              :size="cmpntSize"
              :editable="true"
              :clearable="false"
              :value="datetime"
              format="HH:mm"
              :disabled="isShared"
              @input="(val) => valueChangeHandler('time',val)"
            />
          </template>
          <template v-if="frequence == 'WEEKLY'">
            <span>on</span>
            <el-select
              :value="day"
              :size="cmpntSize"
              placeholder="Day..."
              :disabled="isShared"
              @input="(val) => valueChangeHandler('day',val)"
            >
              <el-option
                v-for="item in dayOptions"
                :key="item.value"
                :label="item.key"
                :value="item.value"
              />
            </el-select>
            <span>at</span>
            <el-time-picker
              arrow-control
              :size="cmpntSize"
              :editable="true"
              :clearable="false"
              :value="datetime"
              format="HH:mm"
              :disabled="isShared"
              @input="(val) => valueChangeHandler('time',val)"
            />
          </template>
          <template v-if="frequence == 'DAILY'">
            <span>at</span>
            <el-time-picker
              arrow-control
              :size="cmpntSize"
              :editable="true"
              :clearable="false"
              :value="datetime"
              format="HH:mm"
              :disabled="isShared"
              @input="(val) => valueChangeHandler('time',val)"
            />
          </template>
          <!-- <template v-if="frequence == 'HOURLY'">
						<el-select
							:value="minute"
							:size="cmpntSize"
							@input="(val) => valueChangeHandler('minute',val)"
							placeholder="Minute...">
							<el-option
							v-for="item in minuteOptions"
							:key="item"
							:label="item"
							:value="item">
							</el-option>
						</el-select>
          </template>-->
          <span
            v-if="frequence != 'HOURLY'"
            class="timezone"
          >MST</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop, Watch, Emit } from 'vue-property-decorator';
import {
  Frequence,
  FrequenceOptions,
  DateOptions,
  DayOptions,
  MinuteOptions,
  TimePickerResult,
} from './index';
import moment from 'moment';
import Util from '@/services/Util.service';

function parseNum(arg: any, defaultVal = 0) {
  const number = parseInt(arg);
  if (!isNaN(number)) {
    return number;
  } else {
    return defaultVal;
  }
}
function momentToDate(m: moment.Moment) {
  const timestamp = parseInt(m.format('x'));
  return new Date(timestamp);
}
function dateToMoment(d: Date) {
  return moment(d.getTime());
}
@Component({
  components: {},
})
export default class TimePikcer extends Vue {
  @Prop()
  value: any;
  @Prop()
  size: 'medium' | 'small' | 'mini';

  @Prop({
    default: false,
  })
  crontab: boolean;

  frequence: Frequence;
  datetime: Date;
  day: string;
  frequenceOption: any;
  dateOptions: any;
  dayOptions: any;
  minuteOptions: any;
  constructor() {
    super();
    this.frequence = 'DAILY';
    this.frequenceOption = FrequenceOptions.filter(i => i.name !== '');
    this.dateOptions = DateOptions;
    this.dayOptions = DayOptions;
    this.minuteOptions = MinuteOptions;
    if (this.value) {
      // this.crontab ? this.parseCron(this.value as string) : this.parseResult(this.value as TimePickerResult);
      // const year = this.value.year ? this.value.year : new Date().getFullYear();
      const currentDate = moment().utcOffset('-07:00');
      const month = this.value.month
        ? this.value.month - 1
        : currentDate.month();
      const date = this.value.dayOfMonths || currentDate.date();
      const day = this.value.dayOfWeeks || 1;
      const hour = this.value.hour || 0;
      const minute = this.value.minute || 0;
      const year = this.value.year || currentDate.year();
      this.frequence = this.value.jobType;
      this.day = day.toString();
      const momentObj = moment()
        .month(month)
        .date(date)
        .hour(hour)
        .minute(minute)
        .second(0);
      this.datetime = momentToDate(momentObj);
    } else {
      this.datetime = new Date(moment().utcOffset(-7).format('YYYY/MM/DD HH:mm:ss'));
      this.day = '1';
    }
    // this.datetime =  new Date();
  }

  get isShared(){
    return Util.isShareApp();
  }

  get input() {
    return this.value;
  }
  get cmpntSize() {
    return this.size;
  }
  get date() {
    return this.datetime.getDate();
  }
  get minute() {
    return this.datetime.getMinutes();
  }

  get repeat() {
    return this.frequence !== 'ONETIME';
  }

  @Watch('frequence')
  onFrequenceChange(newVal: Frequence, oldVal: Frequence) {
    const output = this.crontab
      ? this.getCron(this.frequence)
      : this.getResult(this.frequence);
    this.onInputChange(output);
  }

  @Watch('value')
  onCronChange($val: string | TimePickerResult) {
    this.crontab
      ? this.parseCron($val as string)
      : this.parseResult($val as TimePickerResult);
  }
  @Emit('input')
  onInputChange(result: string | TimePickerResult) {}

  valueChangeHandler(
    type: 'datetime' | 'date' | 'day' | 'time' | 'minute' | 'repeat',
    $val: any
  ) {
    switch (type) {
      case 'datetime':
        this.setDatetime($val);
        break;
      case 'date':
        this.setDate($val);
        break;
      case 'day':
        this.setDay($val);
        break;
      case 'time':
        this.setTime($val);
        break;
      case 'minute':
        this.setMinute($val);
        break;
      case 'repeat':
        this.setFrequence($val);
        break;
    }

    // emit
    const output = this.crontab
      ? this.getCron(this.frequence)
      : this.getResult(this.frequence);
    this.onInputChange(output);
  }
  setFrequence($val: boolean) {
    this.frequence = $val ? 'DAILY' : 'ONETIME';
  }
  setDatetime($val: Date) {
    $val.setSeconds(0);
    this.datetime = $val;
  }
  setDate($val: string) {
    // let newDate = dateToMoment($val);
    const m = dateToMoment(this.datetime).date(parseInt($val));
    this.datetime = momentToDate(m);
  }
  setDay($val: string) {
    this.day = $val;
  }
  setTime($val: any) {
    const newDate = dateToMoment($val);
    const m = dateToMoment(this.datetime)
      .hour(newDate.hour())
      .minute(newDate.minute())
      .second(0);
    this.datetime = momentToDate(m);
  }
  setMinute($val: any) {
    const m = dateToMoment(this.datetime)
      .minute(parseInt($val))
      .second(0);
    this.datetime = momentToDate(m);
  }
  parseCron(cron = '0 0 0 1 1 1 2020') {
    const year = parseNum(cron.split(' ')[6], 2020);
    const month = parseNum(cron.split(' ')[4], 1);
    const date = parseNum(cron.split(' ')[3], 1);
    const day: any = parseNum(cron.split(' ')[5], 1);
    const hour: any = parseNum(cron.split(' ')[2], 1);
    const minute: any = parseNum(cron.split(' ')[1], 1);
    const sec = 0;
    const momentObj = moment()
      .year(year)
      .month(month)
      .date(date)
      .hour(hour)
      .minute(minute)
      .second(0);
    this.datetime = momentToDate(momentObj);
    this.day = day;
  }
  parseResult(result: TimePickerResult) {
    const month = result.month ? result.month - 1 : moment(this.datetime).month();
    const date = result.dayOfMonths || moment(this.datetime).date();
    const day = result.dayOfWeeks || moment(this.datetime).days();
    const hour = result.hour || moment(this.datetime).hour();
    const minute = result.minute || moment(this.datetime).minute();
    const year = result.year || moment(this.datetime).year();
    const momentObj = moment()
      .year(year)
      .month(month)
      .date(date)
      .hour(hour)
      .minute(minute)
      .second(0);
    this.datetime = momentToDate(momentObj);
    this.day = day.toString();
  }
  getCron(frequence: Frequence): string {
    let year = '*';
    let month = '1';
    let date = '1';
    let day: any = '1';
    const time = moment(this.datetime).format('HH:mm');
    const hour: any = parseInt(time.split(':')[0]);
    const minute: any = parseInt(time.split(':')[1]);
    const sec = '0';
    if (frequence == 'ONETIME') {
      year = moment(this.datetime).format('YYYY');
      month = moment(this.datetime).format('MM');
      date = moment(this.datetime).format('DD');
      day = '?';
    } else if (frequence == 'MONTHLY') {
      year = '*';
      month = moment(this.datetime).format('MM');
      date = moment(this.datetime).format('DD');
      day = '?';
    } else if (frequence == 'WEEKLY') {
      year = '*';
      month = '?';
      date = '?';
      day = this.day;
    } else if (frequence == 'DAILY') {
      year = '*';
      month = '*';
      date = moment(this.datetime).format('DD');
      day = '?';
    }
    return `${sec} ${minute} ${hour} ${date} ${month} ${day} ${year}`;
  }
  getResult(frequence: Frequence): TimePickerResult {
    frequence = this.repeat ? frequence : 'ONETIME';
    const year = parseInt(moment(this.datetime).format('YYYY'));
    const month = parseInt(moment(this.datetime).format('MM'));
    const date = parseInt(moment(this.datetime).format('DD'));
    const day = parseInt(this.day);
    const time = moment(this.datetime).format('HH:mm');
    const hour: any = parseInt(time.split(':')[0]);
    const minute: any = parseInt(time.split(':')[1]);
    let result = {
      jobType: frequence,
    } as TimePickerResult;
    switch (frequence) {
      case 'ONETIME':
        result = {
          ...result,
          year: year,
          dayOfMonths: date,
          month: month,
          hour: hour,
          minute: minute,
        } as TimePickerResult;
        break;
      case 'MONTHLY':
        result = {
          ...result,
          dayOfMonths: date,
          hour: hour,
          minute: minute,
        } as TimePickerResult;
        break;
      case 'WEEKLY':
        result = {
          ...result,
          dayOfWeeks: day,
          hour: hour,
          minute: minute,
        } as TimePickerResult;
        break;
      case 'DAILY':
        result = {
          ...result,
          hour: hour,
          minute: minute,
        } as TimePickerResult;
        break;
      case 'HOURLY':
        result = {
          ...result,
          minute: 0,
        } as TimePickerResult;
        break;
    }
    return result;
  }
}
</script>
<style lang="scss" scoped>
.row-info {
  > span {
    align-self: center;
    margin: 0 5px;
    white-space: nowrap;
  }
}
</style>


