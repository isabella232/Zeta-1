import { Vue, Component, Prop, Watch } from 'vue-property-decorator';
import { DataType, PivotData, Aggregators, Record, PlotConfig, GuessType } from './utilities';

@Component
export default class PlotMixin extends Vue {
  @Prop() dataIn!: DataType;
  @Prop() plotConfig!: PlotConfig;

  title: string = 'Plot';
  selectedChartType: string = '';
  chartOptions: string[] = [];

  selectedAggregator: string = '';
  aggregatorNames: string[] = [];


  // allAttrs: string[] = [];
  allAttrsBackup: string[] = [];
  unusedAttrs: string[] = [];
  horizAttrs: string[] = [];
  vertAttrs: string[] = [];

  aggregatorArgs: string[] = [];

  pivotData: PivotData | null = null;

  constructor() {
    super();
  }

  @Watch('inputMetaData', {deep: true})
  onInputMetaData(newVal: {allAttrs: string[], plotConfig: PlotConfig}) {
    const {allAttrs, plotConfig} = newVal;
    /*
    const diff1 = allAttrs.filter( (x) => !this.allAttrsBackup.includes(x) );
    const diff2 = this.allAttrsBackup.filter( (x) => !allAttrs.includes(x) );
    if ( diff1.length === 0 && diff2.length === 0 ) {
      // not a real change.
      return;
    }
    */
    this.initUserInputOptions(newVal);
  }

  @Watch('unusedAttrs')
  onUnusedAttrs(newVal: string[]) {
    for ( let index = 0; index < this.aggregatorArgs.length; index++ ) {
      if ( newVal.indexOf(this.aggregatorArgs[index]) < 0 ) {
        this.aggregatorArgs[index] = newVal[0];
      }
    }
  }

  @Watch('mergedAttrs', {deep: true})
  onMergedAttrsChange(newVal: any) {
    this.invalidOutput();
  }

  /*
  @Watch('currentPlotConfig', {deep: true})
  onCurrentPlotConfigChange(newVal: PlotConfig, oldVal: PlotConfig) {
    this.$emit('config-change', newVal);
  }
  */

  invalidOutput() {
    this.pivotData = new PivotData({
      data: this.dataIn,
      aggregatorName: this.selectedAggregator,
      cols: this.horizAttrs.slice(),
      rows: this.vertAttrs.slice(),
      vals: this.aggregatorArgs.slice()
      });
  }

  beforeMount() {

    this.chartOptions = ['LineCharts', 'BarCharts', 'Table'];
    this.selectedChartType = this.chartOptions[0];
    // this.allAttrs = Array.from(TEST_DATA[0]) as string[];

    // this.aggregatorArgs[0] = this.allAttrs[0];
    // this.unusedAttrs = this.allAttrs.slice();

    this.aggregatorNames = Object.keys(Aggregators);
    this.selectedAggregator = this.aggregatorNames[0];

    this.allAttrsBackup = this.allAttrs.slice();

    this.initUserInputOptions(this.inputMetaData);
  }

  mounted() {
  }

  private guessSuitableAttrIndex(attrs: string[], types: Set<string>): number {
    if ( !attrs ) return -1;
    if ( attrs.length === 0 ) return -1;
    for ( let idx = 0; idx < attrs.length; idx++ ) {
      const attr = attrs[idx];
      const guessType = this.typeGuessed[attr];
      if ( types.has(guessType) ) return idx;
    }
    return -1;
  }

  private initUserInputOptions(meta: {allAttrs: string[], plotConfig: PlotConfig}) {
    const {allAttrs, plotConfig} = meta;
    this.allAttrsBackup = allAttrs.slice();

    this.horizAttrs = (plotConfig.cols || []);
    this.vertAttrs = (plotConfig.rows || []);

    this.horizAttrs = this.horizAttrs.filter( (a) => allAttrs.indexOf(a) >= 0);
    this.vertAttrs = this.vertAttrs.filter( (a) => allAttrs.indexOf(a) >= 0);

    this.selectedAggregator = plotConfig.aggregatorName || this.aggregatorNames[0];
    this.selectedChartType = plotConfig.chartType || this.chartOptions[0];
    this.aggregatorArgs = plotConfig.vals || [];

    // set default horizAttrs and vertAttrs if they are not set, the logic for now
    // is to select the 1st col as horizAttrs and the 2nd col as vertAttrs.
    const attrsExclude = allAttrs.filter( (x) => !((this.horizAttrs.indexOf(x) >= 0) || (this.vertAttrs.indexOf(x)) >= 0));
    if ( Object.keys(plotConfig).length===0 ) {
      let index = this.guessSuitableAttrIndex(attrsExclude, new Set(['date']));
      if ( index === -1 ) index = this.guessSuitableAttrIndex(attrsExclude, new Set(['string']));
      if ( index !== -1 ) this.horizAttrs = attrsExclude.splice(index, 1);
    }

    // if ( Object.keys(plotConfig).length===0 ) {
    //   const index = this.guessSuitableAttrIndex(attrsExclude, new Set(['date', 'string']));
    //   if ( index !== -1 ) this.vertAttrs = attrsExclude.splice(index, 1);
    // }

    // set unusedAttrs
    this.unusedAttrs = allAttrs.filter( (x) => !((this.horizAttrs.indexOf(x) >= 0) || (this.vertAttrs.indexOf(x)) >= 0));
    // make sure aggregator selection has the default value.
    if ( this.aggregatorArgs.length === 0 && this.unusedAttrs.length > 0 ) {
      this.aggregatorArgs = this.unusedAttrs.slice();
    }

    // select a num value for yAxis
    let index = this.guessSuitableAttrIndex(this.aggregatorArgs, new Set(['number']));
    if(index !== -1) {
      let copyAggregators = [...this.aggregatorArgs];
      let numberMetric = copyAggregators.splice(index, 1);
      this.aggregatorArgs = [numberMetric[0], ...copyAggregators];
    }
  }

  /**
   *  add such computed property to remove unnecessary watch event.
   *  Watching on properties separatly will trigger duplicate
   *  events in some cases.
   */
  get mergedAttrs() {
    return {
      horizAttrs: this.horizAttrs,
      vertAttrs: this.vertAttrs,
      selectedAggregator: this.selectedAggregator,
      selectedChartType: this.selectedChartType,
      aggregatorArgs: this.aggregatorArgs,
      dataIn: this.dataIn
    };
  }

  get numberOfAggregatorArgs(): number {
    const aggregator = Aggregators[this.selectedAggregator];
    return aggregator([])().numInputs;
  }

  // calculate all columns from input data.
  get allAttrs() {
    if ( Array.isArray(this.dataIn[0]) ) {
      // first row is the header row.
      return (this.dataIn[0] as string[]);
    }
    // first row is an object, get its keys
    const record = (this.dataIn[0] as Record);
    return Object.keys(record);
  }

  get currentPlotConfig(): PlotConfig {
    // const plotConfig: PlotConfig = {};
    return {
      chartType: this.selectedChartType,
      aggregatorName: this.selectedAggregator,
      cols: this.horizAttrs.slice(),
      rows: this.vertAttrs.slice(),
      vals: this.aggregatorArgs.slice()
    };
  }

  get inputMetaData() {
    const plotConfig = this.plotConfig || {
      cols: [],
      rows: [],
      vals: [],
      aggregatorName: '',
      chartType: ''
    };
    return {
      allAttrs: this.allAttrs,
      plotConfig
    };
  }

  get typeGuessed() {
    let input: Array<string|number>|Record;
    if ( Array.isArray(this.dataIn[0]) ) {
      input = (this.dataIn[1] as Array<string|number>).slice();
    } else {
      input = Object.assign({}, this.dataIn[0] as Record);
    }
    const types = GuessType.guess(input);
    const typeGuessed: {[key: string]: string} = {};
    this.allAttrs.forEach( (key, idx) => {
      typeGuessed[key] = types[idx];
    });
    return typeGuessed;
  }
}
