import { NotebookType } from "./workspace";

export namespace RestPacket {

export interface Error {
    code: string,
    errorDetail: string,
}

export interface File {
    id: string,
    nt: string,
    /* '/' for root */
    path: string,
    content: string,
    title: string,
    createDt: number,
    updateDt: number,
    lastRunDt?: number,
    status: string,
    preference: string,
    opened?:number,
    seq?:number,
    publicReferred?: string | undefined,
    publicRole?: 'ref' | 'pub' | 'no_pub' | null

    nbType: NotebookType
    collectionId?: string
    subNotebooks?: File[],
    packages?: string | null
}

export interface Job {
    id: number,
    notebookId: string,
    content: string,
    creatDt: number,
    startDt: number,
    updateDt: number,
    livySessionId: number,
    livyJobUrl: string,
    status: string,
    preference: string
}

export interface Query {
    id: number;
    requestId: string;
    statement: string;
    seq: number;
    result: string;
    createDt: string;
    startDt: string;
    updateDt: string;
    status: string;
    livySessionId: number;
    livyStatementId: number;
    progress: number;
    proxy_user: string;
}

}
