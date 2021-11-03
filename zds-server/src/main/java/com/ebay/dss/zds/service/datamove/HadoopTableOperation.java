package com.ebay.dss.zds.service.datamove;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.exception.ClusterAccessDeniedException;
import com.ebay.dss.zds.exception.FileStorgeException;
import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.interpreter.InterpreterFactory;
import com.ebay.dss.zds.interpreter.InterpreterManager;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.livy.ZLivySparkSqlInterpreter;
import com.ebay.dss.zds.model.*;
import com.ebay.dss.zds.service.filestorage.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.hive.jdbc.HiveConnection;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
public class HadoopTableOperation {

  private Logger LOGGER = LoggerFactory.getLogger(HadoopTableOperation.class);

  @Autowired
  protected ConfigurationManager configurationManager;

  @Autowired
  private InterpreterFactory interpreterFactory;

  @Autowired
  private InterpreterManager manager;

  @Autowired
  @Qualifier("HDFS")
  private FileStorageService fileStorageService;

  //admin account
  public static final String ADMIN_ACCT = "b_zeta_devsuite";

  private static final String HERMES_UPLOAD_CONF = "hermes.upload.conf";

  public JobResult checkTableExist(Platform platform, String tblName, String nt, String notebookId) {
    String query = String.format("DESC %s", tblName);
    JobResult jobResult;
    String queue = getUserQueue(nt, platform.getId());
    jobResult = executeSparkSQL(nt, platform, queue, query);
    return jobResult;
  }

  public void closeInterpreterSession(String nt, String notebookId) {
    manager.removeNote(nt, notebookId);
  }

  public JobResult executeSparkSQL(String nt, Platform platform, String queue, String query) {
    String notebookId = InterpreterManager.genOnetimeNoteId(nt, platform.getId());
    ZLivySparkSqlInterpreter interpreter = (ZLivySparkSqlInterpreter)
        ((HadoopTableOperation) AopContext.currentProxy())
            .getOrCreateInterpreter(platform, nt, queue, notebookId);
    return executeQuery(notebookId, interpreter, query);
  }

  public String getUserQueue(String nt, int platformId) {
    LOGGER.info("Get User {} queue info in Cluster {}", nt,
        ConfigurationManager.Cluster.valueOfClusterId(platformId));
    List<String> queueInfo = Optional.ofNullable(
        configurationManager.getUserQueueInfo(ConfigurationManager.Cluster
            .valueOfClusterId(platformId), nt)).orElse(Lists.newArrayList());
    queueInfo.removeIf(queue -> queue.equalsIgnoreCase("default")
        || queue.equalsIgnoreCase("hdlq-data-zeta"));
    if (queueInfo.isEmpty()) {
      throw new ClusterAccessDeniedException("You don't have queue access privilege! " +
          "Please apply for access in https://bdp.corp.ebay.com/queue.");
    }
    return queueInfo.get(0);
  }

  public JobResult uploadFileToHdfs(Platform platform, String nt, String localFullPath
      , String hdfsPath, String hdfsFileName) {
    int platformId = isHermesRnoPlatform(platform) ? Platform.apollorno.getId() :
        platform.getId();
    HDFSFileStorage hdfsFileStorage = new HDFSFileStorage(hdfsFileName, hdfsPath, nt, platformId);
    try {
      hdfsFileStorage.setFile(FileUtils.openInputStream(new File(localFullPath)));
      hdfsFileStorage.setOverwrite(true);
      FileStorageResponse fileStorageResponse = fileStorageService.upload(hdfsFileStorage);
      if (fileStorageResponse.isSuccess()) {
        // delete local file
        FileUtils.deleteQuietly(new File(localFullPath));
        return new JobResult(true);
      } else {
        return new JobResult(false, fileStorageResponse.getMsg());
      }
    } catch (IOException e) {
      LOGGER.error("File Upload Failed!", e);
      return new JobResult(false, "File Upload Failed.");
    }
  }

  public JobResult deleteHdfsFile(Platform platform, String nt
      , String hdfsFilePath, String hdfsFileName) {
    int platformId = isHermesRnoPlatform(platform) ? Platform.apollorno.getId() :
        platform.getId();
    HDFSFileStorage hdfsFileStorage = new HDFSFileStorage(hdfsFileName, hdfsFilePath, nt, platformId);
    try {
      FileStorageResponse fileStorageResponse = fileStorageService.delete(hdfsFileStorage);
      if (fileStorageResponse.isSuccess()
          || fileStorageResponse.getCode() == FileStorageResponse.REDIRECT_CODE) {
        return new JobResult(true);
      } else {
        return new JobResult(false, fileStorageResponse.getMsg());

      }
    } catch (FileStorgeException e) {
      LOGGER.error("HDFS File Delete Failed!", e);
      return new JobResult(false, "HDFS File Delete Failed.");
    }
  }

  private JobResult executeQuery(String notebookId, Interpreter interpreter, String query) {
    LOGGER.info("Execute Spark SQL: {}", query);
    ExecutionContext exeContext = new ExecutionContext(notebookId, (int) (Math.random() * 1000));
    List<String> sqlList = Splitter.on(";").trimResults().omitEmptyStrings().splitToList(query);
    for (String sql : sqlList) {
      InterpreterResult interpreterResult = interpreter.interpret(sql, exeContext);
      List<InterpreterResultMessage> messages = interpreterResult.message();
      for (InterpreterResultMessage message : messages) {
        if (interpreterResult.code().equals(InterpreterResult.Code.ERROR)) {
          LOGGER.error("Execute Query Failed : {}", message.getData());
          return new JobResult(false, message.getData());
        }
      }
    }
    return new JobResult(true);
  }

  @Retryable(value = Exception.class, backoff = @Backoff(delay = 120000))
  public Interpreter getOrCreateInterpreter(
      Platform platform, String nt, String queue, String notebookId) {
    LOGGER.info("Get Interpreter NT-{}, NoteId-{}, Platform-{}, Queue-{}",
        nt, notebookId, platform, queue);
    InterpreterConfiguration conf = configurationManager.getDataMoveConfiguration(nt, platform);
    Interpreter interpreter;
    conf.setProperty("zds.livy.spark.yarn.queue", queue);
    interpreter = interpreterFactory.enableLifeCycleMonitor(conf).getInterpreter(nt, notebookId, ZLivySparkSqlInterpreter.class.getName(), conf.getProperties());

    return interpreter;
  }

  private boolean isHermesRnoPlatform(Platform platform) {
    return platform.getId() == Platform.hermes.getId();
  }
}
