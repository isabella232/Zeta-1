import { CodeType, CodeTypes, INotebook, IPreference, IConnection, NotebookType, Notebook, ZPLInterpreterMap, ZPLLivyInterpreter, ZPLParagraph, ZPLNote } from "@/types/workspace";
import _ from "lodash";
import Util from "@/services/Util.service";
import { RestPacket } from "@/types/RestPacket";
import { WorkspaceSrv as NBSrv } from '@/services/Workspace.service';
import { IFile } from "@/types/repository";
import { ElUploadInternalFileDetail, FileUploadStatus, ElUploadInternalRawFile } from "element-ui/types/upload";
import uuid from 'uuid';
import moment from "moment";
export interface InterpreterOptions {
    name: string,
    val: string,
    clusters: Dict<string>
}
export type FileReaderCallback = ((this: FileReader, ev: ProgressEvent) => any) | null
export interface NotebookUploadFormBase {
    folder: string;
    interpreter: string;
    nbType: NotebookType;
}
export interface SingleNotebookForm extends NotebookUploadFormBase {
    name: string;
}
export interface MultiNotebookFrom extends NotebookUploadFormBase{
    fileList: Array<ZetaUploadFileDetail>;
}
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


function getCollectionOptions() {
    return [CodeType.SPARK_PYTHON]
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
function getZeppelinNoteOptions() {
    return _.map(ZPLInterpreterMap, (interpreter: ZPLLivyInterpreter, name: string) => {
        return {
            name,
            val: interpreter
        }
    })
}
export function getInterpreterOptions(nbType: NotebookType): InterpreterOptions[] {
    let result: any[] = [];
    switch (nbType) {
        case 'single':
            result = getNotebookOptions(false);
            break;
        case 'collection':
            result = getNotebookOptions(true);
            break;
        case 'zeppelin':
            result = getZeppelinNoteOptions();
            break;
        // case 'zeppelin-article':
        //     result = [{ name:'', val:'' }];
        //     break;
        case 'zeppelin-dashboard':
            result = [{ name:'Kylin', val:'kylin' }, { name: 'Spark', val: 'hermes'}];
            break;
        case 'zeppelin-native':
            result = [{ name:'Python', val:'python' }];
            break;
    }
    return result;
}
export function getZPLNotebookType(interpreter: ZPLLivyInterpreter): NotebookType{
  let type: NotebookType = 'zeppelin';
  switch (interpreter) {
    case 'kylin':
      type = 'zeppelin-dashboard';
      break;
    case 'python':
      type = 'zeppelin-native';
      break;
    default:
      type = 'zeppelin';
      break;
  }
  return type;
}
export function generateNotebook(form: any, seq: number, opened = 1): Notebook {
    let name = form.name.trim();
    let preference =  <IPreference>{
        "notebook.connection": <IConnection>{
          codeType: form.interpreter
        }}
    return NBSrv.notebook({ name, seq, preference , nbType: form.nbType});
}
export function genereteIFile(nb: INotebook, path: string = '/') {
    return <IFile> {
      notebookId: nb.notebookId,
      nt: Util.getNt(),
      content: "",
      path,
      title: nb.name,
      createTime: nb.createTime,
      updateTime: nb.updateTime,
      state: "",
      preference: undefined,
      selected: false,
      seq: nb.seq,
      nbType: nb.nbType
    };
}
export function generateRestFile(nb: INotebook, path: string = '/'): RestPacket.File {
    return <RestPacket.File> {
        id: nb.notebookId,
        nt: Util.getNt(),
        content: nb.code,
        path,
        title: nb.name,
        createDt: nb.createTime,
        updateDt: nb.updateTime,
        status: "",
        preference: nb.preference ? JSON.stringify(nb.preference) : "",
        opened: 1,
        seq: nb.seq,
        nbType: nb.nbType
    };
}
export function cloneFileFromNotebook(file: RestPacket.File, nb: any, interpreter: string) {
    file.content = nb.content;
    let preference = JSON.parse(nb.preference);
    if (preference.hasOwnProperty("notebook.connection")) {
        preference["notebook.connection"]["batchAccount"] = Util.getNt();
        preference["notebook.connection"]["codeType"] = interpreter;
    } else {
        preference["notebook.connection"] = { "codeType": interpreter, "batchAccount": Util.getNt() };
    }
    file.preference = JSON.stringify(preference);
    return file
}
export class ZetaUploadFileDetail implements ElUploadInternalFileDetail{
    name: string;
    status: FileUploadStatus;
    size: number;
    percentage: number;
    uid: number;
    raw: ElUploadInternalRawFile;
    url?: string | undefined;
    id: string;
    nt: string;
    nameError: string = '';
    path: string
    createTime: number
    preference: IPreference
    interpreter: string;
    content = "";
    fileReader: FileReader
    upload: boolean = false;
    constructor(meta: ElUploadInternalFileDetail, form: any) {
        Object.assign(this, meta);
        this.id = uuid();
        this.nt = Util.getNt();
        this.createTime = moment().valueOf();
        this.interpreter = form.interpreter;
        // this.preference = <IPreference>{
        //     "notebook.connection": <IConnection>{
        //       codeType: form.interpreter
        //     }
        // }
        this.name = this.name.substring(0, this.name.indexOf("."));
        this.path = parseFolder(form.folder)
        this.fileReader = new FileReader();
    }

    buildReader(onerror?: FileReaderCallback, onloadstart?: FileReaderCallback, onprogress?: FileReaderCallback) {
        this.fileReader.onload = e => {
            this.content = (this.fileReader.result || '').toString();
        };
        this.fileReader.onerror = onerror || null;
        this.fileReader.onloadstart = onloadstart || null;
        this.fileReader.onprogress = onprogress || null;
        this.fileReader.readAsText(this.raw);
    }
    get fileName () {
        return this.name.substring(0, this.name.indexOf("."))
    }
    set fileName(name: string) {
        this.name = name
    }
    setPreference(){
        this.preference = <IPreference>{
            "notebook.connection": <IConnection>{
              codeType: this.interpreter
            }
        }
        return this.preference ? JSON.stringify(this.preference) : "";
    }
    toIFile(): IFile {
        return <IFile>{
            notebookId: this.id,
            nt: this.nt,
            content: "",
            path: parseFolder(this.path),
            title: this.name,
            createTime: this.createTime,
            updateTime: this.createTime,
            state: "",
            preference: undefined,
            selected: false,
            nbType: 'single'
        }
    }
    toFile(): RestPacket.File{
        return {
            id: this.id,
            nt: this.nt,
            content: this.content,
            path: parseFolder(this.path),
            title: this.name,
            createDt: this.createTime,
            updateDt: this.createTime,
            status: "",
            preference: this.setPreference(),
            opened: 0,
            seq: -1,
            nbType: 'single'
        }
    }

}
export function getZeppelinNote(form: any) {
  const name = form.name.trim();
  const path = parseFolder(form.folder);
  const interpreter = form.interpreter as ZPLLivyInterpreter;
  const note = {
    name: path + name,
  } as ZPLNote;
  if (interpreter === 'kylin' || interpreter === 'python' || interpreter === 'hermes') {
    note.defaultInterpreterId = interpreter;
  } else {
    const newParagraph = {
      text: `%${interpreter}\n`
    } as ZPLParagraph;
    note.paragraphs = [newParagraph];
  }
  return note;
}
