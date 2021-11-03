package com.ebay.dss.zds.interpreter.output.result;

import com.ebay.dss.zds.interpreter.output.ResultEnum.ResultType;
import com.ebay.dss.zds.interpreter.output.ResultEnum.ResultCode;
import com.google.gson.JsonObject;
import org.apache.zeppelin.interpreter.InterpreterResult;

/**
 * Created by tatian on 2018/4/20.
 */
public abstract class CommonResult implements Result {

    public static final String COL_SEP="\t";

    public static final String ROW_SEP="\n";

    private JsonObject header;

    public ResultCode resultCode;

    public CommonResult(){

    }
    public CommonResult(InterpreterResult itRet) {
        this.header=generateHeader(itRet);
    }

    public JsonObject generateHeader(InterpreterResult itRet){
        JsonObject oj=new JsonObject();
        oj.addProperty("code",getCodeType(itRet.code()).getName());
        resultCode=getCodeType(itRet.code());
        return oj;
    }

    public ResultCode getCodeType(InterpreterResult.Code code){
        ResultCode rcode = null;
        switch (code) {
            case SUCCESS:
                rcode = ResultCode.SUCCESS;break;
            case INCOMPLETE:
                rcode = ResultCode.INCOMPLETE;break;
            case ERROR:
                rcode = ResultCode.ERROR;break;
            case KEEP_PREVIOUS_RESULT:
                rcode = ResultCode.KEEP_PREVIOUS_RESULT;break;
            default:
                rcode = ResultCode.UNSUPPORTED;
        }
        return rcode;
    }
    public ResultType getResultType(InterpreterResult.Type type) {
        ResultType rtype = null;
        switch (type) {
            case TABLE:
                rtype = ResultType.TABLE;break;
            case NULL:
                rtype = ResultType.NULL;break;
            case TEXT:
                rtype = ResultType.TEXT;break;
            case CSV:
                rtype = ResultType.CSV;break;
            case HTML:
                rtype = ResultType.HTML;break;
            case IMG:
                rtype = ResultType.IMG;break;
            case MERGED_MSG:
                /**in most case,we ignore this kind of message for double handle
                 * but can still get from some special method
                 **/
                rtype=ResultType.IGNORED;break;
            default:
                rtype = ResultType.UNSUPPORTED;
        }
        return rtype;
    }

    public abstract Object generateResultList(InterpreterResult itRet);

    public JsonObject getHeader() {
        return header;
    }

    public void setHeader(JsonObject ob){
        this.header=ob;
    }

    @Override
    public ResultCode getResultCode(){
        return this.resultCode;
    }
}
