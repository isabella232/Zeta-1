package com.ebay.dss.zds.common;

import com.ebay.dss.zds.model.ZetaStatus;

/**
 * Created by tatian on 2018/6/12.
 */
public class InterpreterRsp {
    private ZetaStatus zetaStatus;
    private String noteId;
    private String detail;

    public InterpreterRsp(ZetaStatus zetaStatus, String noteId, String detail) {
        this.zetaStatus = zetaStatus;
        this.noteId = noteId;
        this.detail = detail;
    }

    public InterpreterRsp(ZetaStatus zetaStatus, String detail) {
        this.zetaStatus = zetaStatus;
        this.detail = detail;
    }

    public String getNoteId() {
        return noteId;
    }

    public InterpreterRsp setNoteId(String noteId) {
        this.noteId = noteId;
        return this;
    }

    public ZetaStatus getZetaStatus() {
        return zetaStatus;
    }

    public void setZetaStatus(ZetaStatus zetaStatus) {
        this.zetaStatus = zetaStatus;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
