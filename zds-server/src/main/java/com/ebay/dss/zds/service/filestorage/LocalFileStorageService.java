package com.ebay.dss.zds.service.filestorage;

import com.ebay.dss.zds.exception.FileStorgeException;
import com.ebay.dss.zds.model.FileStorage;
import com.ebay.dss.zds.model.FileStorageResponse;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.stream.Stream;

@Service("LOCAL")
public class LocalFileStorageService implements FileStorageService {

  private final static Logger LOGGER = LoggerFactory.getLogger(LocalFileStorageService.class);

  @Override
  public FileStorageResponse read(FileStorage fileStorage) {
    // todo
    return null;
  }

  @Override
  public FileStorageResponse create(FileStorage fileStorage) {
    // todo
    return null;
  }

  @Override
  public FileStorageResponse upload(FileStorage fileStorage) {
    try {
      File targetFile = new File(fileStorage.getFullPath());
      if (!targetFile.getParentFile().exists()) {
        targetFile.getParentFile().mkdirs();
      }
      Files.copy(fileStorage.getFile(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      LOGGER.error("Upload File Failed!", e);
      throw new FileStorgeException("Upload Failed!", e);
    }
    return FileStorageResponse.SUCCESS;
  }

  @Override
  public FileStorageResponse rename(FileStorage fileStorage) {
    //todo
    return null;
  }

  @Override
  public FileStorageResponse delete(FileStorage fileStorage) {
    //todo
    return null;
  }

  @Override
  public Map<String, Object> listFileName(FileStorage fileStorage) {
    Map<String, Object> fileMap = Maps.newLinkedHashMap();
    try {
      Stream<Path> pathStream = Files.list(Paths.get(fileStorage.getFullPath()));
      pathStream.forEach(paths -> fileMap.put(paths.getFileName().toString(), fileStorage.getFullPath()));
    } catch (IOException e) {
      LOGGER.error("List File Failed!", e);
      throw new FileStorgeException("List File Failed!", e);
    }
    return fileMap;
  }
}
