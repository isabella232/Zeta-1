package com.ebay.dss.zds.interpreter.monitor.modle;

public interface Status {

    enum StatusEnum {
        WAITING("Waiting"),
        RUNNING("Running"),
        SUCCESS("Success"),
        FAILED("Failed"),
        UNKNOWN("Unknown"),
        FINISHED("Finished");

        private String name;

        StatusEnum(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static StatusEnum fromString(String status){
            if(status==null) return UNKNOWN;
            switch (status.toLowerCase()){
                case "waiting":return WAITING;
                case "running":return RUNNING;
                case "failed":return FAILED;
                case "unknown":return UNKNOWN;
                case "one job finished":return SUCCESS;
                case "success":return SUCCESS;
                case "finished":return FINISHED;
                default: return UNKNOWN;
            }
        }
    }

    StatusEnum getStatus();

    Status setStatus(StatusEnum status);

    Status setProgress(double progress);

    double getProgress();
}
