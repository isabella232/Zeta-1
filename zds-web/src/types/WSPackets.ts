export namespace WSPacket {

export interface Message {
    op: string,
    data: any
}

export type Any = any;

/**
 * Req: request client sends to server
 * Res: response server sends to client
 */
export interface JobReq {
    notebookId: string,
    reqId: string,
    sqls: string
}

export interface JobRes {
    notebookId: string,
    jobId: string,
    reqId: string
}

export type JobReadyRes = JobRes;
export type JobDoneRes  = JobRes;

export interface SplittedRes {
    notebookId: string,
    jobId: string,
    reqId: string,
    codes: string[]
}

export interface JobErrorRes {
    notebookId: string,
    errors: string[],
    jobId: string,
    reqId: string,
    code: CodeWithSeq
}

export interface QueryStart {
    noteId: string,
    jobId: string,
    seq: number,
    startDt: number
    zetaStatementKey:number
};

export interface QueryProgressRes {
    notebookId: string,
    jobId: string,
    reqId: string,
    code: CodeWithSeq,
    // progress: number,
    sparkJobUrl: string
    status: any
}

export type ResultTable = {
    type: string,
    schema: string[],
    rows: Array<{ [K: string]: string }>;
    count?: number;
    updatedCount?: number;
};
export type ResultContent = {
    type: 'TABLE' | 'HTML' | 'TEXT' | 'IMG',
    content: string;
};

export interface ResultField {
    header: { code: string },
    result: Array<ResultTable | ResultContent>
}

export interface QueryResultRes {
    notebookId: string,
    jobId: string,
    reqId: string,
    code: CodeWithSeq,
    startDt?: number,
    endDt: number,
    /* result should be parsed into { header, rows } */
    result: string
}

export interface InteralErrorRes {
    code: string,
    detail: string
}

/* -- */

export interface CodeWithSeq {
    code: string,
    seq: number,
    statementId?: number,
}

export interface ConnectProgressRes {
    userName: string,
    noteId: string,
    interpreter: string,
    progress: number,
    message?: string
}

export enum OP {
  /* OPs from server */
  NB_CODE_INVALID_NOTEBOOK_STATUS =    "NB_CODE_INVALID_NOTEBOOK_STATUS",
  INTERNAL_ERROR =                     "INTERNAL_ERROR",
  NB_CODE_EXECUTE_ERROR =              "NB_CODE_EXECUTE_ERROR",
  NB_CODE_STATEMENT_SUCCESS =          "NB_CODE_STATEMENT_SUCCESS",
  NB_CODE_JOB_READY =                  "NB_CODE_JOB_READY",
  NB_CODE_JOB_DONE =                   "NB_CODE_JOB_DONE",
  NB_CODE_PREPROCESSED =               "NB_CODE_PREPROCESSED",
  NB_CODE_STATEMENT_PROGRESS =         "NB_CODE_STATEMENT_PROGRESS",
  NB_CODE_SESSION_EXPIRED =            "NB_CODE_SESSION_EXPIRED",
  NB_CODE_STATEMENT_START =            "NB_CODE_STATEMENT_START",
	NB_CODE_DUMP_DONE = 			 	 "NB_CODE_DUMP_DONE",

	CONNECTION_ERROR = 					 "CONNECTION_ERROR",
  CONNECTION_SUCCESS = 				 "CONNECTION_SUCCESS",
  CONNECTION_ABORT =                   "CONNECTION_ABORT",
  CONNECTION_PROGRESS =                "CONNECTION_PROGRESS",
	DISCONNECTION_SUCCESS = 			 "DISCONNECTION_SUCCESS",
	DISCONNECTION_ERROR = 				 "DISCONNECTION_ERROR",
	CANCEL_SUCCESS = 					 "CANCEL_SUCCESS",
  CANCEL_ERROR = 						 "CANCEL_ERROR",
  GREETING = 			                 "GREETING",


    /* OPs to server */
	NB_CODE_JOB_SUBMIT = "NB_CODE_SUBMIT",
	NB_CODE_JOB_DUMP = 'NB_CODE_DUMP',
	NB_CODE_JOB_CANCEL = "NB_JOB_CANCEL",
	NB_CONNECT = 'NB_CONNECT',
  NB_DISCONNECT = 'NB_DISCONNECT',
  ZETA_SERVER_REQ_ID = 'ZETA_SERVER_REQ_ID',
  NB_RECOVER = "NB_RECOVER"
}

}
