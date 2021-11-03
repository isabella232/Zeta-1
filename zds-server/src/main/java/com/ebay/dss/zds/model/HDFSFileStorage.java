package com.ebay.dss.zds.model;


public class HDFSFileStorage extends FileStorage {

  public HDFSFileStorage() {

  }

  public HDFSFileStorage(String fileName, String path, String nt, int clusterID) {
    super(fileName, path, nt);
    this.clusterId = clusterID;
  }

  @Deprecated
  public HDFSFileStorage(ZetaPackage zetaPackage) {
    this.fileName = zetaPackage.getFileName();
    this.nt = zetaPackage.getNt();
    this.clusterId = zetaPackage.getCluster();
    this.fullPath = zetaPackage.getFilePath() + zetaPackage.getFileName();
    this.type = zetaPackage.getType();
  }

  int clusterId;

  public int getClusterId() {
    return clusterId;
  }

  public void setClusterId(int clusterId) {
    this.clusterId = clusterId;
  }

  @Override
  public String toString() {
    return "HDFSFileStorage [fileName=" + fileName + ", fullPath=" + fullPath + ", nt=" + nt +
        ", isOverwrite=" + isOverwrite + ", renamePath=" + renamePath + ", clusterId=" + clusterId + ", type=" + type + "]";
  }
}
