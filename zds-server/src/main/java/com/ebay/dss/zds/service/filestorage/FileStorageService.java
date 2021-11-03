package com.ebay.dss.zds.service.filestorage;


import com.ebay.dss.zds.model.FileStorage;
import com.ebay.dss.zds.model.FileStorageResponse;

import java.util.Map;

public interface FileStorageService {

  FileStorageResponse read(FileStorage fileStorage);

  FileStorageResponse create(FileStorage fileStorage);

  FileStorageResponse upload(FileStorage fileStorage);

  FileStorageResponse rename(FileStorage fileStorage);

  FileStorageResponse delete(FileStorage fileStorage);

  Map<String,Object> listFileName(FileStorage fileStorage);

}
