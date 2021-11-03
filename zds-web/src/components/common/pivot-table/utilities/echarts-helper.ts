import { PivotData } from '../utilities';

class EChartHelper {

    getDefaultChartGrid() {
        return {
            top:30,
            left: 100,
            right:100,
            bottom:50
        };
    }

    getDefaultToolbox() {
        return {
            show: true,
            right: 10,
            feature: {
                dataZoom: {
                    yAxisIndex: 'none',
                    title: {
                      zoom: 'Zoom in',
                      back: 'Zoom out'
                    }
                },
                dataView: {show: false},
                magicType: {show: false},
                restore: {
                  title: 'Restore'
                },
                saveAsImage: {show: false}
            }
        };
    }

    getAxisLabelName(plotData: PivotData) {
        let xAxisLabel = plotData.props.cols!.join(" + ");
        let yAxisLabel = plotData.props.vals![0];
        let formattedxAxisLabel = '';
        const maxLength = 12;
        const group = Math.ceil(xAxisLabel.length / maxLength);
        for(let i = 0; i < group; i++) {
            formattedxAxisLabel += xAxisLabel.substring(i * maxLength, i * maxLength + maxLength) + '\n';
        }
        switch(plotData.props.aggregatorName) {
            case 'Count':
                yAxisLabel = 'Count(#)';
                break;
            case 'None':
                break;
            case 'Sum':
                yAxisLabel = `Sum(${yAxisLabel})`;
                break;
            case 'Integer Sum':
                yAxisLabel =  `Integer Sum(${yAxisLabel})`
                break;
        }
        return {
          xAxis: formattedxAxisLabel,
          yAxis: yAxisLabel
        };
    }
}

let echartsHelper = new EChartHelper();
export default echartsHelper;