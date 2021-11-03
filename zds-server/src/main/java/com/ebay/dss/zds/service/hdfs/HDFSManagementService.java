package com.ebay.dss.zds.service.hdfs;

import com.ebay.dss.zds.exception.FileStorgeException;
import com.ebay.dss.zds.model.FileStorageResponse;
import com.ebay.dss.zds.model.HDFSFileStorage;
import com.ebay.dss.zds.service.filestorage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Created by tatian on 2019-12-09.
 */
@Service
public class HDFSManagementService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HDFSManagementService.class);

  @Autowired
  @Qualifier("HDFS")
  private FileStorageService fileStorageService;

  public FileStorageResponse read(String nt, String path, int cluster) {
    HDFSFileStorage hdfsFileStorage = new HDFSFileStorage();
    hdfsFileStorage.setNt(nt);
    hdfsFileStorage.setFullPath(path);
    hdfsFileStorage.setClusterId(cluster);
    return fileStorageService.read(hdfsFileStorage);
  }

  public FileStorageResponse create(String nt, String path, int cluster) {
    HDFSFileStorage hdfsFileStorage = new HDFSFileStorage();
    hdfsFileStorage.setNt(nt);
    hdfsFileStorage.setFullPath(path);
    hdfsFileStorage.setClusterId(cluster);
    return fileStorageService.create(hdfsFileStorage);
  }

  public FileStorageResponse upload(String nt, String filePath, String fileName, int cluster, boolean isOverWrite, MultipartFile file) {
    if (file.isEmpty()) {
      throw new FileStorgeException("File %s is empty");
    }
    try {
      HDFSFileStorage hdfsFileStorage = new HDFSFileStorage();
      hdfsFileStorage.setNt(nt);
      hdfsFileStorage.setFileName(fileName);
      hdfsFileStorage.setFullPath(filePath + "/" + fileName);
      hdfsFileStorage.setClusterId(cluster);
      hdfsFileStorage.setOverwrite(isOverWrite);
      hdfsFileStorage.setFile(file.getInputStream());
      return fileStorageService.upload(hdfsFileStorage);
    } catch (IOException e) {
      LOGGER.error("Read File Failed!", e);
      throw new FileStorgeException("Upload Failed", e);
    }
  }

  public FileStorageResponse rename(String nt, String oldPath, String newPath, int cluster) {
    HDFSFileStorage hdfsFileStorage = new HDFSFileStorage();
    hdfsFileStorage.setNt(nt);
    hdfsFileStorage.setFileName(oldPath);
    hdfsFileStorage.setFullPath(oldPath);
    hdfsFileStorage.setRenamePath(newPath);
    hdfsFileStorage.setClusterId(cluster);
    return fileStorageService.rename(hdfsFileStorage);
  }

  public FileStorageResponse delete(String nt, String path, int cluster) {
    HDFSFileStorage hdfsFileStorage = new HDFSFileStorage();
    hdfsFileStorage.setNt(nt);
    hdfsFileStorage.setFullPath(path);
    hdfsFileStorage.setClusterId(cluster);
    return fileStorageService.delete(hdfsFileStorage);
  }

  public Map<String, Object> listFolder(String nt, String path, int cluster) {
    HDFSFileStorage hdfsFileStorage = new HDFSFileStorage();
    hdfsFileStorage.setNt(nt);
    hdfsFileStorage.setFullPath(path);
    hdfsFileStorage.setClusterId(cluster);
    return fileStorageService.listFileName(hdfsFileStorage);
  }
}
