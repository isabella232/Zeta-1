import _ from "lodash";
import { RestPacket } from "@/types/RestPacket";
import { IFile } from "@/types/repository";

export function fileMapper(src: IFile): RestPacket.File {
    let file = <RestPacket.File>{
        id: src.notebookId,
        nt: src.nt,
        path: src.path,
        content: src.content || undefined,
        title: src.title,
        createDt: src.createTime,
        updateDt: src.updateTime,
        status: src.state,
        preference: JSON.stringify(src.preference)
    };
    let r = _.omitBy(file, val => !val);
    return r as any as RestPacket.File;
}