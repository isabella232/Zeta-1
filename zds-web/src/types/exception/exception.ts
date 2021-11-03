import uuid from 'uuid';
import _ from 'lodash';
import { ZETA_EXCEPTION_TAG } from './tags';
export interface ZetaExceptionProps {
  path: 'notebook' | 'repository' | 'release' | 'schedule' | 'settings'
  | 'da' | 'metadata' | 'registervdm'
  | 'sqlGuide' | 'datamove' | 'datavalidation' | 'favorite'
  | 'unknown' | 'navigator' | 'zeta';

  workspaceId?: string;
  [k: string]: any;
}
export interface StackTrace {
  methodName: string;
  fileName: string;
  lineNumber: number;
  className: string;
  nativeMethod: boolean;
}
export interface Rule {
  id: number;
  order: number;
  filter: string;
  message: string;
  errorCode: string;
  createDt: number;
  updateDt: number;
}
export interface ErrorDetail {
  action?: object;
  message: string;
  originalMessage?: string;
  cause?: {
    errorCode?: string;
    cause: any;
    stackTrace: StackTrace[];
    message: string;
    localizedMessage: string;
    suppressed: any[];
    [key: string]: any;
  };
  context?: {
    errorCode?: string;
    context: any;
    stackTrace: StackTrace[];
    message: string;
    localizedMessage: string;
    suppressed: any[];
    [key: string]: any;
  };
  rule?: Rule;
}
export interface WSErrorDetail extends ErrorDetail {
  cause: {
    errorCode?: string;
    cause: any;
    stackTrace: StackTrace[];
    message: string;
    localizedMessage: string;
    suppressed: any[];
    [key: string]: any;
  };
  context: {
    errorCode?: string;
    context: any;
    stackTrace: StackTrace[];
    message: string;
    localizedMessage: string;
    suppressed: any[];
    [key: string]: any;
  };
}

export enum ExceptionType {
  ERROR = 'ERROR',
  WARNING = 'WARNING'
}
export interface ExceptionPacket{
  type?: ExceptionType;
  code: string;
  errorDetail: ErrorDetail;
}
export interface WSExceptionPacket extends ExceptionPacket {
  errorDetail: WSErrorDetail;
  responseHeaders?: any;
}
export class ZetaException {
  /**
     * exception id
     */
  id: string;

  /**
     *
     */
  code: string;
  message: string;
  originalMessage: any;
  cause: any;
  context: any;
  errorCode: string;
  causeMessage: string;
  stackTrace: StackTrace[];
  rule: Rule;
  responseHeaders: any;

  /**
     *
     */
  private front_end_tags_: ZETA_EXCEPTION_TAG[];
  properties: ZetaExceptionProps;

  useHtml = false;
  resolved: boolean;
  createTime: number;
  /**
     * front end error code
     */
  feCode?: string;
  originalResponse: any;

  /**
   * exception type
   */
  type: ExceptionType;

  constructor(e: ExceptionPacket, props: ZetaExceptionProps = { path: 'unknown' }, responseHeader?: any) {

    this.id = `exctn_id_${uuid()}`;
    try {
      this.code = e.code;
      if (e.errorDetail) {
        this.message = e.errorDetail.message;
        this.originalMessage = e.errorDetail.originalMessage;
        if(e.errorDetail.cause) {
          this.cause = e.errorDetail.cause.cause;
          if(e.errorDetail.cause.cause){
            this.cause.zetaStatus = e.errorDetail.cause.zetaStatus?e.errorDetail.cause.zetaStatus:'';
          }
          this.errorCode = e.errorDetail.cause.errorCode || '';
          this.causeMessage = e.errorDetail.cause.message;
          this.stackTrace = e.errorDetail.cause.stackTrace;
        }
        if (e.errorDetail.rule) {
          this.rule = e.errorDetail.rule;
        }
        if (e.errorDetail.context) {
          this.context = e.errorDetail.context;
          this.errorCode = e.errorDetail.context.errorCode || '';
          this.causeMessage = e.errorDetail.context.message;
          this.stackTrace = e.errorDetail.context.stackTrace;
        }
      }
    } catch (e) {
      if (!this.message) {
        this.message = 'Unknown error';
      }
    }
    this.properties = props;
    this.resolved = false;
    this.front_end_tags_ = [];
    this.createTime = new Date().getTime();
    this.type = e.type ? e.type : ExceptionType.ERROR;
    this.responseHeaders = responseHeader;
  }
  private setFrontEndTags(c: ZETA_EXCEPTION_TAG | ZETA_EXCEPTION_TAG[]) {
    if (Array.isArray(c)) {
      this.front_end_tags_ = this.front_end_tags_.concat(c);
      if (_.includes(c, ZETA_EXCEPTION_TAG.INVISIBLE_ERR)) {
        this.setResolved();
      }
    } else {
      this.front_end_tags_.push(c);
      if (c === ZETA_EXCEPTION_TAG.INVISIBLE_ERR) {
        this.setResolved();
      }
    }
  }
  private setResolved(r = true) {
    this.resolved = r;
  }

  public hasTag(c: ZETA_EXCEPTION_TAG) {
    return _.includes(this.front_end_tags_, c);
  }

  /**
     * chain function
     */
  public resolve(rv = true): ZetaException {
    this.setResolved(rv);
    return this;
  }
  public tags(c?: ZETA_EXCEPTION_TAG | ZETA_EXCEPTION_TAG[]): ZetaException {
    if (c) {
      this.setFrontEndTags(c);
    }
    return this;

  }
  public props(props: Dict<any>) {
    Object.assign(this.properties, props);
    return this;
  }
  public resHeaders(responseHeaders: any) {
    if (responseHeaders) {
      if (!this.responseHeaders) {
        this.responseHeaders = {'zds-server-req-id': ''};
      }
      Object.assign(this.responseHeaders, responseHeaders);
    }
    return this;
  }
  public template(html: string) {
    if (html) {
      this.useHtml = true;
      this.message = html;
    }
    return this;

  }
  /**
     * toString
     */
  public toString() {
    return JSON.stringify(this);
  }

}
export function isExceptionPacket(data: any) {
  const { code, errorDetail } = data;
  if (data && code && errorDetail) {
    return true;
    // const { message, cause, rule } = errorDetail;
    // if(message) {
    //     const { message, cause: subCause, stackTrace } = cause;
    //     if(message || subCause || stackTrace) {
    //         return true
    //     }
    // }
  }
  return false;
}
export function customZetaException(msg: string, data: any, props?: ZetaExceptionProps): ZetaException {
  const packet: ExceptionPacket = {
    code: '',
    errorDetail: {
      message: msg,
      cause: {
        message: JSON.stringify(data),
        cause: '',
        suppressed: [] as any[],
        localizedMessage: '',
        stackTrace: [] as StackTrace[],
      },
      context: {
        message: JSON.stringify(data),
        context: '',
        suppressed: [] as any[],
        localizedMessage: '',
        stackTrace: [] as StackTrace[],
      },
    },
  };
  const ex = new ZetaException(packet, props);
  ex.originalResponse = data;
  return ex;
}
