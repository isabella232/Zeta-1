import { Record } from './pivot-data';
import { IAgg } from './interfaces';

// -- type and interfaces
interface IAggregator {
  push: (record?: Record ) => void;
  value: () => any;
  format: FormatterType;
  numInputs: number;
}

type FormatterType = (value: number) => string;

// -- help functions and methods
const toNumberOrNaN = (value: string | number): number => {
  if ( typeof value === 'string' ) {
    return Number(value); // change from parseFloat to Number since parseFloat("3asc") will return 3 which is not correct
  }
  if ( typeof value === 'number' ) {
    return value;
  }
  return NaN;
};
// addSeparators("1213.322", ",", ".") => 1,213.322
const addSeparators = (nStr: string, thousandsSep: string, decimalSep: string) => {
  const x = nStr.split('.');
  let x1 = x[0];
  const x2 = x.length > 1 ? decimalSep + x[1] : '';
  const rgx = /(\d+)(\d{3})/;
  while (rgx.test(x1)) {
    x1 = x1.replace(rgx, `$1${thousandsSep}$2`);
  }
  return x1 + x2;
};

const numberFormat = (optsIn?: {[key: string]: string|number})  => {
  const defaults = {
    digitsAfterDecimal: 2,
    scaler: 1,
    thousandsSep: ',',
    decimalSep: '.',
    prefix: '',
    suffix: '',
  };
  const opts = Object.assign({}, defaults, optsIn);
  return (x: number) => {
    if (isNaN(x) || !isFinite(x)) {
      return '';
    }
    const result = addSeparators(
      (opts.scaler * x).toFixed(opts.digitsAfterDecimal),
      opts.thousandsSep,
      opts.decimalSep
    );
    return `${opts.prefix}${result}${opts.suffix}`;
  };
};

// -- aggregator classes
class DefaultAggregator implements IAggregator {
  val: number = 0;
  attr: string | undefined;
  format: FormatterType;
  numInputs: number;

  constructor(format: FormatterType, attr?: string ) {
    this.format = format;
    this.numInputs = typeof attr !== 'undefined' ? 0 : 1;
    this.attr = attr;
  }

  push(record?: Record) {
    if (this.val !== 0) { return; }
    const numberValue = toNumberOrNaN(record![this.attr!]);
    if (!isNaN(numberValue)) { this.val = numberValue; }
  }

  value() {
    return this.val;
  }
}
class CountAggregator implements IAggregator {
  count: number = 0;
  numInputs: number;
  format: FormatterType;
  constructor( format: FormatterType ) {
    this.format = format;
    this.numInputs = 0;
  }
  push(record?: Record) {
    this.count++;
  }
  value() {
    return this.count;
  }
}

class UniqueAggregator implements IAggregator {
  uniq: Array<(string|number)> = [];
  format: FormatterType;
  fn: ([]) => any;
  attr: string | undefined;
  numInputs: number;

  constructor(format: FormatterType, fn: ([]) => any, attr?: string ) {
    this.format = format;
    this.fn = fn;
    this.attr = attr;
    this.numInputs = typeof this.attr !== 'undefined' ? 0 : 1;
  }

  push(record?: Record) {
    if (this.uniq.indexOf(record![this.attr!]) < 0) {
      this.uniq.push(record![this.attr!]);
    }
  }
  value() {
    return this.fn(this.uniq);
  }
}

class SumAggregator implements IAggregator {
  sum: number = 0;
  attr: string | undefined;
  format: FormatterType;
  numInputs: number;

  constructor(format: FormatterType, attr?: string ) {
    this.format = format;
    this.numInputs = typeof attr !== 'undefined' ? 0 : 1;
    this.attr = attr;
  }

  push(record?: Record) {
    const numberValue = toNumberOrNaN(record![this.attr!]);
    if (!isNaN(numberValue)) { this.sum += numberValue; }
  }

  value() {
    return this.sum;
  }
}

// aggregator templates default to US number formatting but this is overrideable
const usFmt = numberFormat();
const usFmtInt = numberFormat({digitsAfterDecimal: 0});
const usFmtPct = numberFormat({
  digitsAfterDecimal: 1,
  scaler: 100,
  suffix: '%',
});

const aggregatorTemplates = {
  count(formatter = usFmtInt) {
    return () =>
      (): IAggregator => {
        return new CountAggregator(formatter);
      };
  },

  uniques(fn: (arg: any) => any, formatter = usFmtInt) {
    return ([attr]: string[]) => {
      return (): IAggregator => {
        return  new UniqueAggregator(formatter, fn, attr);
      };
    };
  },

  sum(formatter = usFmt) {
    return ([attr]: string[]) => {
      return (): IAggregator => {
        return  new SumAggregator(formatter, attr);
      };
    };
  },
  default(formatter = usFmt) {
    return ([attr]: string[]) => {
      return (): IAggregator => {
        return  new DefaultAggregator(formatter, attr);
      };
    };
  }
};

const Aggregators: {[key: string]: (args: string[]|undefined) => (() => IAggregator)} = ((tpl: any) => ({
  'Sum': tpl.sum(usFmt),
  'None': tpl.default(usFmt),
  'Count': tpl.count(usFmtInt),
  'Integer Sum': tpl.sum(usFmtInt)
}))(aggregatorTemplates);

export { Aggregators, IAggregator };
