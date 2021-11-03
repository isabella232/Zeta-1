import _ from 'lodash'
class GuessTypeImpl {
  dateFormats: RegExp[] = [
    /^\d{8}$/,
    /^\d{2}[-/.]\d{2}[-/.]\d{4}$/,
    /^\d{4}[-/.]\d{2}[-/.]\d{2}$/
  ];

  guess(input: Array<string|number> | {[key: string]: string|number}): string[] {
    if ( Array.isArray(input) ) {
      const arrayInput: Array<string|number> = input as Array<string|number>;
      return arrayInput.map( (value) => this.guessType(value) );
    } else {
      return _.values(input).map( (value) => this.guessType(value) );
    }
  }

  private guessType(input: string|number) {
    if ( typeof input === 'number' ) { return 'number'; }
    if ( typeof input === 'undefined') { return 'string'; } // undefined is treated as string
    if ( typeof input === 'string' ) {
      const strInput = input as string;
      if(/^\d+(\.\d+)?$/.test(strInput)) {
        return 'number';
      } else {
        if ( strInput.length !== 8 && strInput.length !== 10 ) {
          return 'string';
        }
        const found = this.dateFormats.find( (r) => r.test(strInput), undefined );
        if ( found !== undefined ) { return 'date'; }
      }
    }
    return 'string';
  }
}

const GuessType = new GuessTypeImpl();
export default GuessType;
