export interface ZPLPackages {
  packages: Array<{
    type: string;
    body: {
      clusterId: number;
      path: string;
    };
  }>;
}
export type ZPLLivyInterpreter = 'pyspark' | 'sql' | 'livy.spark' | 'sparkr' | 'python' | 'kylin' | 'hermes';
export const ZPLInterpreterMap: Dict<ZPLLivyInterpreter> = {
  'PySpark': 'pyspark',
  'Spark SQL': 'sql',
  'Scala': 'livy.spark',
  'SparkR': 'sparkr',
  // 'Kylin': 'kylin',
  // 'Python': 'python'
};
export interface ZPLParagraph {
  text: string;
}
export interface ZPLInterpreterSetting {
  id: string;
  interpreters: any[];
  name: string;
  selected: boolean;
}
export interface ZPLNote {
  packages?: string;
  defaultInterpreterId?: string;
  name: string;
  id: string;
  opened: number;
  seq: number;
  interpreterSettings: ZPLInterpreterSetting[];
  createDt?: number;
  updateDt?: number;
  lastRunDt?: number;
  paragraphs?: Array<ZPLParagraph>;
}
export type ZPLMessageHandler = (msg: ZPLMessage) => void;
export type ZPLAction = 'ZPL_USED_CAPACITY_DIALOG' |  'ZPL_USED_CAPACITY' | 'ZPL_CONFIG_QUEUE' | 'ZPL_UPLOAD_JAR' | 'ZPL_GET_KYLIN_PJCTS' | 'ZPL_GET_NOTE_FVRT' | 'ZPL_SET_NOTE_FVRT' | 'ZPL_GET_BATCH_ACCOUNT'
| 'ZPL_SHARE_NOTE' | 'ZPL_CLONE_NOTE' | 'ZPL_SEND_EL_MSG';
export interface ZPLMessage {
  action: ZPLAction;
  noteId: string;
  params?: Dict<any>;
}
