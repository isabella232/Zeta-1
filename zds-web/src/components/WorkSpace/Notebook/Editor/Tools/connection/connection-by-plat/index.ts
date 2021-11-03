import { SparkConnection, SparkConnectionConstructor } from './connection-spark-adaptee';
import { TdConnection, TdConnectionConstructor } from './connection-td-adaptee';
import { PySparkConnection, PySparkConnectionConstructor } from './connection-pyspark-adaptee';
import { KylinConnection, KylinConnectionConstructor } from './connection-kylin-adaptee';
import { HiveConnection, HiveConnectionConstructor } from './connection-hive-adaptee';
import { CodeType } from '@/types/workspace';

export { ConnectionAdaptee } from './connection-adaptee';
export type ConnectionConstructor = SparkConnectionConstructor | TdConnectionConstructor | KylinConnectionConstructor | HiveConnectionConstructor | PySparkConnectionConstructor
export const getConnectionAdaptee = (codeType: CodeType) => {
    let constructor: ConnectionConstructor = SparkConnection;
    switch(codeType) {
        case CodeType.SQL:
            constructor = SparkConnection;
            break;
        case CodeType.TERADATA:
            constructor = TdConnection;
			      break;
        case CodeType.KYLIN:
            constructor = KylinConnection;
            break;
        case CodeType.HIVE:
            constructor = HiveConnection;
            break;
        case CodeType.SPARK_PYTHON:
            constructor = PySparkConnection;
            break;
    }
    return constructor;
}
