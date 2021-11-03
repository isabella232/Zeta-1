import _ from "lodash";
import Util from "@/services/Util.service";
import { ElUploadInternalFileDetail, FileUploadStatus, ElUploadInternalRawFile } from "element-ui/types/upload";
import uuid from 'uuid';
import moment from "moment";

export type FileReaderCallback = ((this: FileReader, ev: ProgressEvent) => any) | null

export interface MultiPackageForm{
    fileList: Array<ZetaUploadFileDetail>;
    cluster: number;
    isOverWrite: boolean;
    path: string;
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
export class ZetaUploadFileDetail implements ElUploadInternalFileDetail{
    name: string;
    fileName: string;
    fileType: string;
    filePath: string;
    nameError: string = '';
    status: FileUploadStatus;
    size: number;
    percentage: number;
    uid: number;
    raw: ElUploadInternalRawFile;
    url?: string | undefined;
    id: string;
    nt: string;
    createTime: number;
    fileReader: FileReader
    constructor(meta: ElUploadInternalFileDetail, raw?: ElUploadInternalRawFile) {
        Object.assign(this, meta);
        this.id = uuid();
        this.nt = Util.getNt();
        this.createTime = moment().valueOf();
        if(raw){
            this.name = raw.name;
            this.size = raw.size;
            this.raw = raw;
            this.status = 'ready';
        }
        this.fileName = this.name.substring(0, this.name.lastIndexOf("."));
        this.fileType = this.name.substring(this.name.lastIndexOf(".")+1,this.name.length);
        this.fileReader = new FileReader();
    }

    buildReader(onerror?: FileReaderCallback, onloadstart?: FileReaderCallback, onprogress?: FileReaderCallback) {
        this.fileReader.onerror = onerror || null;
        this.fileReader.onloadstart = onloadstart || null;
        this.fileReader.onprogress = onprogress || null;
    }
}