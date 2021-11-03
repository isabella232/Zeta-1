package com.ebay.dss.zds.model;


import java.io.InputStream;

public class FileStorage {

  InputStream file;

  String fileName;

  String fullPath;

  String nt;

  boolean isOverwrite;

  String renamePath;

  String type;

  Short permission = (short) 00700;

  public FileStorage() {

  }

  public FileStorage(String fileName, String path, String nt) {
    this.fileName = fileName;
    this.fullPath = path + fileName;
    this.nt = nt;
  }

  public InputStream getFile() {
    return file;
  }

  public void setFile(InputStream file) {
    this.file = file;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFullPath() {
    return fullPath;
  }

  public void setFullPath(String fullPath) {
    this.fullPath = fullPath;
  }

  public String getNt() {
    return nt;
  }

  public void setNt(String nt) {
    this.nt = nt;
  }

  public boolean isOverwrite() {
    return isOverwrite;
  }

  public void setOverwrite(boolean overwrite) {
    isOverwrite = overwrite;
  }

  public String getRenamePath() {
    return renamePath;
  }

  public void setRenamePath(String renamePath) {
    this.renamePath = renamePath;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Short getPermission() {
    return permission;
  }

  public void setPermission(Short permission) {
    this.permission = permission;
  }

  @Override
  public String toString() {
    return "HDFSFileStorage [fileName=" + fileName + ", fullPath=" + fullPath + ", nt=" + nt + ", isOverwrite=" + isOverwrite + ", renamePath=" + renamePath + ", type=" + type + "]";
  }
}
