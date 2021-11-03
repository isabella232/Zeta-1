package com.ebay.dss.zds.websocket.notebook.dto;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Created by tatian on 2018/5/31.
 */
public class ExecuteCodeCancelRsp implements Serializable {

    public enum STATUS{
        CANCELLING,
        CANCELLED,
        FAILED
    }
    @NotBlank
    private String notebookId;
    @NotBlank
    private String jobId;
    @NotBlank
    private String reqId;
    @NotBlank
    private String status;
    private String info;

    public ExecuteCodeCancelRsp(String notebookId, String jobId, String reqId, String status){
        this.notebookId=notebookId;
        this.jobId=jobId;
        this.reqId=reqId;
        this.status=status;
    }

    public String getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(String notebookId) {
        this.notebookId = notebookId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }
}
