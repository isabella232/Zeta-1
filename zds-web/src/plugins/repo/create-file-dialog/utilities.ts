import _ from "lodash";
import { CodeType, CodeTypes, INotebook, IPreference, IConnection, NotebookType, Notebook } from "@/types/workspace";
import Util from "@/services/Util.service";
import { InterpreterOptions, ICreateOption, ISingleNotebookForm } from ".";
import { IFile } from "@/types/repository";
import uuid from 'uuid';
import moment from 'moment';

export function parseFolder(path: string) {
    if (path && path.length > 0) {
        const lastChar = path[path.length - 1];
        if (lastChar !== '/') {
            path = path + '/'
        }
        const firstChar = path[0]
        if (firstChar !== '/') {
            path = '/' + path;
        }
    }
    return path
}
export function getType(name: any) {
    let type = CodeType.SQL;
    switch (name) {
        case CodeType.SQL:
            type = CodeType.SQL;
            break;
        case CodeType.TERADATA:
            type = CodeType.TERADATA;
            break;
        case CodeType.KYLIN:
            type = CodeType.KYLIN;
            break;
        case CodeType.HIVE:
            type = CodeType.HIVE;
            break;
        case CodeType.SPARK_PYTHON:
            type = CodeType.SPARK_PYTHON;
            break;
    }
    return type;
}
function getNotebookOptions(isCollection: boolean) {
    return _.chain(CodeTypes).map((ct, name) => {
        if(ct.isCollection !== isCollection) {
            return undefined
        }
        let type = getType(name);
        return  {
            name: ct.name,
            val: name,
            clusters: Util.getDefaultConnectionByPlat(type).clustersMap
        }
        
    }).compact().value()
}

export function getInterpreterOptions(nbType: NotebookType): InterpreterOptions[] {
    let result: any[] = [];
    switch (nbType) {
        case 'single':
            result = getNotebookOptions(false)
            break;
        case 'collection':
            result = getNotebookOptions(true)
            break;
    }
    return result;
}
export function genereteIFile({ name, folder, seq, nbType } :  ( { seq: number} & ISingleNotebookForm)) {
    return <IFile> {
      notebookId: uuid(),
      nt: Util.getNt(),
      content: "",
      path: folder,
      title: name,
      createTime: moment().valueOf(),
      updateTime: moment().valueOf(),
      state: "",
      preference: undefined,
      selected: false,
      seq: seq,
      nbType: nbType
    };
}