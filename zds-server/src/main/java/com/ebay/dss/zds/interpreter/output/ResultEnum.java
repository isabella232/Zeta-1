package com.ebay.dss.zds.interpreter.output;

/**
 * Created by tatian on 2018/4/20.
 */
public class ResultEnum {
    public enum ResultType {
        TEXT("TEXT"),
        CSV("CSV"),
        TABLE("TABLE"),
        NULL("NULL"),
        HTML("HTML"),
        IMG("IMG"),
        IGNORED("IGNORED"),
        UNSUPPORTED("UNSUPPORTED");

        private String name;
        ResultType(String name){
            this.name=name;
        }
        public String getName(){return this.name;}
    }

    public enum ResultCode {
        SUCCESS("SUCCESS"),
        INCOMPLETE("INCOMPLETE"),
        ERROR("ERROR"),
        KEEP_PREVIOUS_RESULT("KEEP_PREVIOUS_RESULT"),
        UNSUPPORTED("UNSUPPORTED");

        private String name;
        ResultCode(String name){
            this.name=name;
        }
        public String getName(){return this.name;}
    }
}
