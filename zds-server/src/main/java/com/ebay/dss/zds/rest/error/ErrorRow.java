package com.ebay.dss.zds.rest.error;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class ErrorRow {
    int level;
    String message;
    Class classType;
    @JsonIgnore
    Throwable t;
    String className;
    String fullClassName;

    public ErrorRow() {
    }

    public ErrorRow(int level, String message, Throwable t) {
        this.level = level;
        this.message = message;
        this.t = t;
        this.classType = t.getClass();
        this.className = this.classType.getSimpleName();
        this.fullClassName = this.classType.getName();
    }

    public static List<ErrorRow> errorRowsFromThrowable(Throwable t) {
        List<ErrorRow> rows = new ArrayList<ErrorRow>();
        int level = 0;
        while (true) {
            if (t == null) break;
            ErrorRow errorRow = new ErrorRow(level, t.getMessage(), t);
            rows.add(errorRow);
            level++;
            t = t.getCause();
        }
        return rows;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Class getClassType() {
        return classType;
    }

    public void setClassType(Class classType) {
        this.classType = classType;
    }

    public Throwable getT() {
        return t;
    }

    public void setT(Throwable t) {
        this.t = t;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getFullClassName() {
        return fullClassName;
    }

    public void setFullClassName(String fullClassName) {
        this.fullClassName = fullClassName;
    }

    @Override
    public String toString() {
        return "ErrorRow{" +
                "level=" + level +
                ", message='" + message + '\'' +
                ", classType=" + classType +
                ", t=" + t +
                ", className='" + className + '\'' +
                ", fullClassName='" + fullClassName + '\'' +
                '}';
    }
}
