package com.ebay.dss.zds.interpreter.output.result;

import com.ebay.dss.zds.interpreter.output.ResultEnum.ResultCode;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResultMessage;

/**
 * Created by tatian on 2018/4/20.
 */
public interface Result {

    public Object parse(InterpreterResultMessage message);

    public String get();

    public void set(InterpreterResult itRet);

    public ResultCode getResultCode();
}
