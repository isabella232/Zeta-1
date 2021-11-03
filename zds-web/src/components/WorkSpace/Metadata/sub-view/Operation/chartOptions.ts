import moment from 'moment';
import _ from 'lodash';

export const Green = 'rgba(114,181,128)';
export const Blue = 'rgba(61,137,242)';
export const Orange = 'rgba(221,164,81)';
export const Yellow = 'rgba(253,233,198)';
export const FontColor = 'rgba(128,84,37)';
export const Gray = 'rgba(138,150,177)';

export const tableStatus =  [
  { name: 'Pending', color: Gray },
  { name: 'WIP', color: Blue },
  { name: 'Finish', color: Green },
];

export const Last30DaysLabel = Array.from({ length: 30 }, (_, i) => {
  return moment().subtract(30 - i, 'day').format('MM/DD');
});

export function labelFormatter(ts: number) {
  return moment(ts).utcOffset('-0700').format('HH:mm:ss');
}

export function getLabelByStatus(status: string) {
  switch(status) {
    case 'not ready':
      return {
        backgroundColor: Gray,
      };
    case 'unknown':
      return {
        backgroundColor: '#FFF',
        color: FontColor,
        borderColor: '#ccc',
        borderWidth: 1,
      };
    case 'ready':
      return {
        backgroundColor: Green,
      };
    case 'warning':
      return {
        backgroundColor: Blue,
      };
    
  }
}

export function getHistoryData(data) {
  const map = _.keyBy(data, i => moment(i.finish_time).format('MM/DD'));
  return Last30DaysLabel.map(l => {
    const record = map[l];
    return record ? moment(record.finish_time.split(' ')[1], 'HH:mm:ss.SZ').valueOf() : null;
  });
}

export function getHistoryDataFormatFloat(data) {
  const map = _.keyBy(data, i => moment(i.finish_time).utcOffset('-0700').format('MM/DD'));
  return Last30DaysLabel.map(l => {
    const record = map[l];
    return record ? moment(moment(record.finish_time).utcOffset('-0700').format('YYYY-MM-DD HH:mm:ss')).hour() + Math.round((moment(moment(record.finish_time).utcOffset('-0700').format('YYYY-MM-DD HH:mm:ss')).minute() * 100) / 60) / 100 : null;
  });
}

export function getFailureData(data) {
  const map = _.keyBy(data, i => moment(i.date).format('MM/DD'));
  return Last30DaysLabel.map(l => {
    const record = map[l];
    return record ? record.failure_rate : null;
  });
}

export const treeInitOptions = {
  series:[
    {
      type: 'tree',
      name: 'tree',
      orient: 'RL',
      top: '5%',
      left: '10%',
      bottom: '3%',
      right: '16%',
      symbol: 'path://M1600 736v192q0 40-28 68t-68 28h-416v416q0 40-28 68t-68 28h-192q-40 0-68-28t-28-68v-416h-416q-40 0-68-28t-28-68v-192q0-40 28-68t68-28h416v-416q0-40 28-68t68-28h192q40 0 68 28t28 68v416h416q40 0 68 28t28 68z',
      symbolOffset:[-30, 0],
      symbolSize: (_, { data }) => {
        if (data.expandable && data.children.length === 0) {
          return 12;
        }
        return 0.01;
      },
      itemStyle: {
        borderColor: Green,
        color: Green,
      },
      lineStyle: {
        width: 1,
      },
      label: {
        formatter({ name, data }) {
          const status = data.info && data.info.status;
          if (status === 'unknown') {
            return `${name} {unknown|?}`;
          }
          if (status === 'warning') {
            return `${name} {warning|!}`;
          }
          return name;
        },
        rich: {
          unknown: {
            backgroundColor: Yellow,
            borderRadius: 999,
            color: FontColor,
            verticalAlign: 'center',
            align: 'center',
            padding: 2,
            width: 8,
            height: 8,
            fontSize: 10,
          },
          warning: {
            backgroundColor: Orange,
            borderRadius: 999,
            color: '#fff',
            verticalAlign: 'center',
            align: 'center',
            padding: 2,
            width: 8,
            height: 8,
            fontSize: 10,
          },
        },
        backgroundColor: Green,
        color: '#fff',
        padding: [6, 12],
        position: 'right',
        borderRadius: 4,
      },
      expandAndCollapse: false,
    },
  ],
};

export const lineInitOptions = {
  tooltip: {
    trigger: 'axis',
    formatter(params) {
      const colorSpan = color => '<span style="display:inline-block;margin-right:5px;border-radius:10px;width:9px;height:9px;background-color:' + color + '"></span>';
      let html = '<p>' + params[0].axisValue + '</p>';
      params.forEach(item => {
        const line = '<p>' + colorSpan(item.color) + ' ' + item.seriesName + ': ' + labelFormatter(item.data) + '</p>';
        html += line;
      });
      return html;
    },
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: Last30DaysLabel,
  },
  yAxis: {
    type: 'value',
    min: (value) => {
      return value.min - 6000000;
    },
    max: (value) => {
      return value.max + 6000000;
    },
    axisLabel: {
      formatter: labelFormatter,
    },
  },
  grid: {
    left: '5%',
  },
};