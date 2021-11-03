package com.ebay.dss.zds.service;


import com.ebay.dss.zds.dao.ZetaPackageRepository;
import com.ebay.dss.zds.exception.FileStorgeException;
import com.ebay.dss.zds.model.FileStorageResponse;
import com.ebay.dss.zds.model.HDFSFileStorage;
import com.ebay.dss.zds.model.ZetaPackage;
import com.ebay.dss.zds.service.filestorage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ZetaPackageService {

  private final static Logger LOGGER = LoggerFactory.getLogger(ZetaPackageService.class);

  @Autowired
  @Qualifier("HDFS")
  private FileStorageService fileStorageService;

  @Autowired
  private ZetaPackageRepository zetaPackageRepository;

  public FileStorageResponse uploadPackage(String nt, String fileType, String filePath, String fileName, int cluster, boolean isOverWrite, MultipartFile file) {
    if (file.isEmpty()) {
      throw new FileStorgeException(String.format("File %s is empty", fileName));
    }
    /*
    List<ZetaPackage> zetaPackageList = zetaPackageRepository.findByFileNameAndNtAndCluster(fileName, nt, cluster);
    if ((!isOverWrite) && zetaPackageList.size() > 0) {
      return FileStorageResponse.REDIRECT;
    }
     */
    try {
     /*
     ZetaPackage zetaPackage = zetaPackageList.size() > 0 ?
          zetaPackageList.get(0) : new ZetaPackage();
      zetaPackage.setCluster(cluster);
      zetaPackage.setFileName(fileName);
      zetaPackage.setFilePath(String.format("/user/%s/", nt));
      zetaPackage.setNt(nt);
      zetaPackage.setType(fileType);
      zetaPackage.setCreateTime(new Date());
      */

      HDFSFileStorage hdfsFileStorage = new HDFSFileStorage(fileName, filePath, nt, cluster);
      hdfsFileStorage.setType(fileType);
      hdfsFileStorage.setOverwrite(isOverWrite);
      hdfsFileStorage.setFile(file.getInputStream());
      FileStorageResponse fileStorageResponse = fileStorageService.upload(hdfsFileStorage);

      /*
      if (fileStorageResponse.isSuccess()) {
        //update db
        zetaPackageRepository.save(zetaPackage);
      }
       */
      return fileStorageResponse;
    } catch (IOException e) {
      LOGGER.error("Read File Failed!", e);
      throw new FileStorgeException("Upload Failed", e);
    }

  }

  public FileStorageResponse renamePackage(String nt, Long id, String newName) {
    ZetaPackage zetaPackage = zetaPackageRepository.findById(id).orElseThrow(() -> new FileStorgeException("The file is not exist!"));
    if (zetaPackageRepository.findByFileNameAndNt(newName, nt).size() > 0) {
      return new FileStorageResponse(FileStorageResponse.FAIL_CODE, "The name have been existed.");
    }

    HDFSFileStorage hdfsFileStorage = new HDFSFileStorage(zetaPackage);
    hdfsFileStorage.setRenamePath(zetaPackage.getFilePath() + newName);
    FileStorageResponse fileStorageResponse = fileStorageService.rename(hdfsFileStorage);

    if (fileStorageResponse.isSuccess()) {
      //update db
      zetaPackage.setFileName(newName);
      zetaPackage.setUpdateTime(new Date());
      zetaPackageRepository.save(zetaPackage);
    } else if (fileStorageResponse.getCode() == FileStorageResponse.REDIRECT_CODE) {
      zetaPackageRepository.delete(zetaPackage);
      return new FileStorageResponse(FileStorageResponse.FAIL_CODE, String.format("File %s is not exist on HDFS.", hdfsFileStorage.getFileName()));
    }
    return fileStorageResponse;
  }


  public FileStorageResponse deletePackage(String nt, Long id) {
    Optional<ZetaPackage> zetaPackage = zetaPackageRepository.findById(id);
    if (!zetaPackage.isPresent()) {
      return FileStorageResponse.SUCCESS;
    }

    HDFSFileStorage hdfsFileStorage = new HDFSFileStorage(zetaPackage.get());
    FileStorageResponse fileStorageResponse = fileStorageService.delete(hdfsFileStorage);

    if (fileStorageResponse.isSuccess() || fileStorageResponse.getCode() == FileStorageResponse.REDIRECT_CODE) {
      //update db
      zetaPackageRepository.delete(zetaPackage.get());
    }
    return fileStorageResponse;
  }


  @Deprecated
  public List<ZetaPackage> getPackages(String nt) {
    return zetaPackageRepository.findByNt(nt);
  }

  public Map<String, Object> getHDFSFolderList(String nt, int cluster, String path) {
    HDFSFileStorage hdfsFileStorage = new HDFSFileStorage();
    hdfsFileStorage.setNt(nt);
    hdfsFileStorage.setClusterId(cluster);
    hdfsFileStorage.setFullPath(path);

    return fileStorageService.listFileName(hdfsFileStorage);
  }

  public List<String> getFilePathById(List<Long> ids) {
    List<ZetaPackage> zetaPackages = zetaPackageRepository.findByIdIn(ids);
    return zetaPackages.stream()
        .map(zetaPackage -> zetaPackage.getFilePath() + zetaPackage.getFileName())
        .collect(Collectors.toList());
  }

  public List<ZetaPackage> getPackageById(List<Long> ids) {
    return zetaPackageRepository.findByIdIn(ids);
  }
}
