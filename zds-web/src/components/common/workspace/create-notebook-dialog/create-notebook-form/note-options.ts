import _ from 'lodash';
type LivyInterpreter = 'Spark SQL' | 'PySpark' | 'Scala' | 'SparkR';
type Interpreter = 'Spark SQL' | 'TD SQL' | 'Hive SQL' | 'Python' | 'Kylin' | LivyInterpreter;
type NoteType = 'solo' | 'stacked';
interface NoteOption {
  interpreter: Interpreter;
  type: NoteType;
  platform?: Dict<string>;
  description?: string;
}
const noteOptions: NoteOption[] = [
  {
    interpreter: 'Spark SQL',
    type: 'solo',
    description: 'Available platform: Ares,ApolloReno,Hercules,Hermes',
  },
  // zeppelin.hermes
  {
    interpreter: 'Spark SQL',
    type: 'stacked',
    platform: {
      'hermes': 'Hermes',
      'apollo': 'Apollo',
    }
  },
  {
    interpreter: 'TD SQL',
    type: 'solo',
    description: 'Available platform: Mozart',
  },
  {
    interpreter: 'Hive SQL',
    type: 'solo',
    description: 'Available platform: Ares,Hercules,ApolloReno',
  },
  {
    interpreter: 'Python',
    type: 'stacked',
    platform: {
      'zeta': 'Zeta',
    }
  },
  {
    interpreter: 'Kylin',
    type: 'solo',
    platform: {
      'kylin': 'Apollo',
    }
  },
  {
    interpreter: 'Kylin',
    type: 'stacked',
    platform: {
      'kylin': 'Apollo',
    }
  },
  {
    interpreter: 'PySpark',
    type: 'stacked',
    platform: {
      'apollo': 'Apollo',
    }
  },
  {
    interpreter: 'Scala',
    type: 'stacked',
    platform: {
      'apollo': 'Apollo',
    }
  },
  {
    interpreter: 'SparkR',
    type: 'stacked',
    platform: {
      'apollo': 'Apollo',
    }
  },
];
const interpreterOptions: Interpreter[] = _.chain(noteOptions)
  .map(option => option.interpreter).sort().uniq().value();

const noteOptionByInterpreter = (interpreter: Interpreter) => {
  return _.chain(noteOptions)
    .filter( option => option.interpreter === interpreter)
    .sortBy('interpreter')
    .value();
};
export {
  Interpreter,
  NoteType,
  NoteOption,
  noteOptions,
  interpreterOptions,
  noteOptionByInterpreter,
};
