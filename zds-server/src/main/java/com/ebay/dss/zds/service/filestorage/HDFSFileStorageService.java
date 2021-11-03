package com.ebay.dss.zds.service.filestorage;

import com.ebay.dss.zds.exception.FileStorgeException;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClientBuilder;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyMessage;
import com.ebay.dss.zds.model.FileStorage;
import com.ebay.dss.zds.model.FileStorageResponse;
import com.ebay.dss.zds.model.HDFSFileStorage;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.zeppelin.livy.LivyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service("HDFS")
public class HDFSFileStorageService implements FileStorageService {

  private final static Logger LOGGER = LoggerFactory.getLogger(HDFSFileStorageService.class);


  private int init(LivyClient livyClient, String nt) {
    LOGGER.info("Init FileSession for {}", nt);
    try {
      LivyMessage.FileSessionInfo fileSessionInfo = livyClient.createFileSession(new LivyMessage.CreateFileSessionRequest(nt));
      return fileSessionInfo.id;
    } catch (LivyException e) {
      LOGGER.error("Connect File Session Failed!", e);
      throw new FileStorgeException("Connect to Hdfs failed!", e);
    }

  }

  private LivyClient buildLivyClient(HDFSFileStorage hdfsFileStorage) {
    return new LivyClientBuilder().withClusterId(hdfsFileStorage.getClusterId(), true).build();
  }

  private boolean checkExist(int fileSessionId, LivyClient livyClient, HDFSFileStorage hdfsFileStorage) throws LivyException {
    return livyClient.checkExist(fileSessionId, new LivyMessage.FileCheckRequest(hdfsFileStorage.getFullPath())).exist;
  }

  @Override
  public FileStorageResponse read(FileStorage fileStorage) {
    HDFSFileStorage hdfsFileStorage = (HDFSFileStorage) fileStorage;
    LOGGER.info("Get file {} from HDFS", hdfsFileStorage.getFullPath());
    LivyClient livyClient = buildLivyClient(hdfsFileStorage);
    int fileSessionId = init(livyClient, hdfsFileStorage.getNt());
    LOGGER.info("Init FileSession {}", fileSessionId);
    LOGGER.info("Get file from path: {}", hdfsFileStorage.getFullPath());
    try {
      LivyMessage.FileGetResponse fileGetResponse =
              livyClient.getFileV2(fileSessionId,
                      new LivyMessage.FileGetRequest(hdfsFileStorage.getFullPath()));
      FileStorageResponse<byte[]> fileStorageResponse;
      if (fileGetResponse.success) {
        fileStorageResponse = new FileStorageResponse<>(FileStorageResponse.SUCCESS_CODE, "", fileGetResponse.getAsByte());
      } else {
        throw new LivyException(fileGetResponse.error);
      }
      return fileStorageResponse;
    } catch (LivyException e) {
      LOGGER.error("Get Failed!", e);
      throw new FileStorgeException("Get Failed", e);
    } finally {
      close(livyClient, fileSessionId);
    }
  }

  /** Create path
   *  the fileName field should be empty, so the path will be created
   **/
  @Override
  public FileStorageResponse create(FileStorage fileStorage) {
    HDFSFileStorage hdfsFileStorage = (HDFSFileStorage) fileStorage;
    LOGGER.info("Create Path {} to HDFS", hdfsFileStorage.getFullPath());
    LivyClient livyClient = buildLivyClient(hdfsFileStorage);
    int fileSessionId = init(livyClient, hdfsFileStorage.getNt());
    LOGGER.info("Init FileSession {}", fileSessionId);
    LOGGER.info("Create Path: {}", hdfsFileStorage);
    try {
      if ((!hdfsFileStorage.isOverwrite()) && checkExist(fileSessionId, livyClient, hdfsFileStorage)) {
        LOGGER.info("Path {} exist on HDFS", hdfsFileStorage.getFullPath());
        return FileStorageResponse.REDIRECT;
      }
      LivyMessage.PathCreateResponse pathCreateResponse =
              livyClient.createPath(fileSessionId,
                      new LivyMessage.PathCreateRequest(hdfsFileStorage.getFullPath(), hdfsFileStorage.getPermission()));
      return pathCreateResponse.success ?
              FileStorageResponse.SUCCESS :
              new FileStorageResponse(FileStorageResponse.FAIL_CODE, pathCreateResponse.error);
    } catch (LivyException e) {
      LOGGER.error("Create Failed!", e);
      throw new FileStorgeException("Create Failed", e);
    } finally {
      close(livyClient, fileSessionId);
    }
  }

  @Override
  public FileStorageResponse upload(FileStorage fileStorage) {
    HDFSFileStorage hdfsFileStorage = (HDFSFileStorage) fileStorage;
    LOGGER.info("Upload file {} to HDFS", hdfsFileStorage.getFileName());
    LivyClient livyClient = buildLivyClient(hdfsFileStorage);
    int fileSessionId = init(livyClient, hdfsFileStorage.getNt());
    LOGGER.info("Init FileSession {}", fileSessionId);
    LOGGER.info("Upload File: {}", hdfsFileStorage);
    try {
      if ((!hdfsFileStorage.isOverwrite()) && checkExist(fileSessionId, livyClient, hdfsFileStorage)) {
        LOGGER.info("File {} exist on HDFS", hdfsFileStorage.getFileName());
        return FileStorageResponse.REDIRECT;
      }
      byte[] buffer = new byte[hdfsFileStorage.getFile().available()];
      hdfsFileStorage.getFile().read(buffer);
      LivyMessage.FileUploadResponse fileUploadResponse = livyClient.uploadFileV2(fileSessionId, new LivyMessage.FileUploadRequestV2(buffer, hdfsFileStorage.getFullPath(), true));
      return fileUploadResponse.success ? FileStorageResponse.SUCCESS : new FileStorageResponse(FileStorageResponse.FAIL_CODE, fileUploadResponse.error);
    } catch (LivyException | IOException e) {
      LOGGER.error("Upload Failed!", e);
      throw new FileStorgeException("Upload Failed", e);
    } finally {
      close(livyClient, fileSessionId);
    }
  }

  @Override
  public FileStorageResponse rename(FileStorage fileStorage) {
    HDFSFileStorage hdfsFileStorage = (HDFSFileStorage) fileStorage;
    LOGGER.info("Rename file {} on HDFS", hdfsFileStorage.getFileName());
    LivyClient livyClient = buildLivyClient(hdfsFileStorage);
    int fileSessionId = init(livyClient, fileStorage.getNt());
    LOGGER.info("Init FileSession {}", fileSessionId);
    LOGGER.info("Rename File: {}", hdfsFileStorage);

    try {
      if (!checkExist(fileSessionId, livyClient, hdfsFileStorage)) {
        LOGGER.info("File {} not exist on HDFS", hdfsFileStorage.getFileName());
        return FileStorageResponse.REDIRECT;
      }
      LivyMessage.FileRenameResponse fileRenameResponse = livyClient.renameFile(fileSessionId, new LivyMessage.FileRenameRequest(hdfsFileStorage.getFullPath(), hdfsFileStorage.getRenamePath()));
      return fileRenameResponse.success ? FileStorageResponse.SUCCESS : new FileStorageResponse(FileStorageResponse.FAIL_CODE, fileRenameResponse.error);
    } catch (LivyException e) {
      LOGGER.error("Rename Failed!", e);
      throw new FileStorgeException("Rename Failed", e);
    } finally {
      close(livyClient, fileSessionId);
    }
  }

  @Override
  public FileStorageResponse delete(FileStorage fileStorage) {
    HDFSFileStorage hdfsFileStorage = (HDFSFileStorage) fileStorage;
    LivyClient livyClient = buildLivyClient(hdfsFileStorage);
    int fileSessionId = init(livyClient, fileStorage.getNt());
    LOGGER.info("Init FileSession {}", fileSessionId);
    LOGGER.info("Delete File: {}", hdfsFileStorage);
    try {
      if (!checkExist(fileSessionId, livyClient, hdfsFileStorage)) {
        LOGGER.info("File {} not exist on HDFS", hdfsFileStorage.getFileName());
        return FileStorageResponse.REDIRECT;
      }
      LivyMessage.FileDeleteResponse fileDeleteResponse = livyClient.deleteFile(fileSessionId, new LivyMessage.FileDeleteRequest(hdfsFileStorage.getFullPath()));
      return fileDeleteResponse.deleted ? FileStorageResponse.SUCCESS : new FileStorageResponse(FileStorageResponse.FAIL_CODE, fileDeleteResponse.error);
    } catch (LivyException e) {
      LOGGER.error("Delete Failed!", e);
      throw new FileStorgeException("Delete Failed", e);
    } finally {
      close(livyClient, fileSessionId);
    }
  }

  @Override
  public Map<String, Object> listFileName(FileStorage fileStorage) {
    HDFSFileStorage hdfsFileStorage = (HDFSFileStorage) fileStorage;
    LivyClient livyClient = buildLivyClient(hdfsFileStorage);
    int fileSessionId = init(livyClient, hdfsFileStorage.getNt());
    LOGGER.info("Init FileSession {}", fileSessionId);
    Map fileList = Maps.newLinkedHashMap();
    try {
      LivyMessage.FolderListResponse folderListResponse = livyClient.folderList(fileSessionId, new LivyMessage.FolderListRequest(hdfsFileStorage.getFullPath()));
      folderListResponse.fileStatus.stream().filter(hadoopFileStatus -> !getFileName(hadoopFileStatus.path).startsWith(".")).forEach(hadoopFileStatus ->
          fileList.put(hadoopFileStatus.path, hadoopFileStatus)
      );
    } catch (LivyException e) {
      LOGGER.error("Get Folder List Failed!", e);
      throw new FileStorgeException("Get Folder List Failed", e);
    } finally {
      close(livyClient, fileSessionId);
    }
    return fileList;
  }

  private String getFileName(String path) {
    List<String> paths = Splitter.on("/").splitToList(path);
    return Strings.nullToEmpty(paths.get((paths.size() > 0 ? paths.size() : 1) - 1));
  }

  private void close(LivyClient livyClient, int fileSessionId) {
    LOGGER.info("Close FileSession for {}", fileSessionId);
    livyClient.closeFileSession(fileSessionId);
  }
}
