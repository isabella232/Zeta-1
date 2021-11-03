package com.ebay.dss.zds.interpreter.api;

import com.ebay.dss.zds.common.ClusterUtil;
import com.ebay.dss.zds.common.InterpreterRsp;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.exception.InterpreterException;
import com.ebay.dss.zds.exception.InterpreterServiceException;
import com.ebay.dss.zds.exception.InterpreterStoppedException;
import com.ebay.dss.zds.exception.LifeCycleException;
import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.interpreter.InterpreterManager;
import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.interpreter.interpreters.HDFSInterpreter;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.interpreter.interpreters.jdbc.JdbcConf;
import com.ebay.dss.zds.message.EventTracker;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.model.ZetaResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Properties;

import static com.ebay.dss.zds.exception.ErrorCode.INTERPRETER_CONNECT_EXCEPTION;
import static com.ebay.dss.zds.interpreter.InterpreterType.EnumType.IMITATE;
import static com.ebay.dss.zds.interpreter.interpreters.jdbc.Constant.JDBC_CONF_KEY;
import static com.ebay.dss.zds.message.event.Event.*;
import static com.ebay.dss.zds.model.ZetaStatus.*;

/**
 * Created by tatian on 2018/5/20.
 */
@Service
public class InterpreterService {

    private static final Logger logger = LoggerFactory.getLogger(InterpreterService.class);

    private ConfigurationManager confManager;

    private ZetaNotebookRepository zetaNotebookRepository;

    private InterpreterManager manager;

    @Autowired
    public InterpreterService(ConfigurationManager confManager,
                              ZetaNotebookRepository zetaNotebookRepository,
                              InterpreterManager interpreterManager) {
        this.confManager = confManager;
        this.zetaNotebookRepository = zetaNotebookRepository;
        this.manager = interpreterManager;
    }

    public String removeNote(String userName, String noteId) {
        return manager.removeNote(userName, noteId);
    }

    public String removeNoteAsyncOrNot(String userName, String noteId, boolean async) {
        return manager.removeNoteAsyncOrNot(userName, noteId, async);
    }

    private Interpreter reopenInterpreter(String userId,
                                   String noteId,
                                   String className,
                                   InterpreterConfiguration conf) throws LifeCycleException, InterpreterException {
        // InterpreterConfiguration conf = confManager.getConfiguration(noteId, className, proxyUser, clusterId, prop);
        return manager.getFactory()
                .enableLifeCycleMonitor(conf)
                .reopenInterpreter(userId, noteId, className, conf.getProperties());
    }

    public Interpreter openInterpreter(String userId, String noteId, String className, Properties prop) throws InterpreterException {
        return manager.getFactory()
                .reopenInterpreter(userId, noteId, className, prop);
    }

    private InterpreterConfiguration getConfiguration(String userName,
                                        String noteId,
                                        String interpreter,
                                        Map<String, String> prop) {
        return confManager.prepareConfiguration(userName, noteId, interpreter, prop);
    }

    public ZetaResponse<InterpreterRsp> openNote(String userName,
                                                 String noteId,
                                                 String interpreter,
                                                 Map<String, String> prop) {

        InterpreterConfiguration finalProp = getConfiguration(userName, noteId, interpreter, prop);

        ZetaResponse<InterpreterRsp> response;
        InterpreterType.EnumType intpType = InterpreterType.fromString(interpreter);
        switch (intpType) {
            case LIVY_SHARED:
            case LIVY_SPARK:
            case LIVY_SPARKR:
            case LIVY_PYSPARK:
            case LIVY_SPARKSQL:
                response = reopenLivyApp(userName, noteId, interpreter, finalProp);
                break;
            case JDBC:
                response = openJDBC(userName, noteId, finalProp.getProperties());
                break;
            case SSH_RESTRICTED_HDFS:
                response = openHDFS(userName, noteId, finalProp.getProperties());
                break;
            case IMITATE:
                response = openNote(userName, IMITATE.getName(), noteId, finalProp.getProperties());
                break;
            default:
                throw new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION,
                        INTP_SESSION_CONNECTION_ERROR, noteId, "Unknown interpreter type: " + interpreter);
        }
        return response;
    }

    private ZetaResponse<InterpreterRsp> reopenLivyApp(String userName,
                                               String noteId,
                                               String interpreter,
                                               InterpreterConfiguration conf) {

        Properties prop = conf.getProperties();
        String interpreterClassName = InterpreterType.toClass(interpreter).getName();
        // Integer clusterId = Integer.valueOf(prop.getProperty("clusterId"));
        String proxyUser = prop.getProperty("proxyUser");
        String preference = prop.getProperty("preference");

        ZetaEvent zce = ZetaConnectionEvent(userName, noteId, interpreterClassName).addProperties(prop);
        try {//always reopen
            //logger.info("Got reopen request from " + userName + " as " + proxyUser);
            EventTracker.postEvent(zce);
            // only update notebook preference if it's owner and ignore if the notebook is not exists
            zetaNotebookRepository.updateNotebookPreferenceByIdAndNt(noteId, preference, userName);
            reopenInterpreter(userName, noteId, interpreterClassName, conf);

            //logger.info("Succeed to open interpreter for user: {} of note: {} with proxy user: {}", userName, noteId, proxyUser);
            EventTracker.postEvent(ZetaConnectionSuccessEvent(userName, noteId, interpreterClassName, zce.getRecordTime()).addProperties(prop));

            return new ZetaResponse<>(new InterpreterRsp(INTP_SESSION_OPENED, "Note: " + noteId + " is opened"), HttpStatus.OK);
        } catch (InterpreterException e) {
            removeNote(userName, noteId);

            //logger.info("Failed to open interpreter for user: {} of note: {} with proxy user: {} log: {}", userName, noteId, proxyUser, e.getMessage());
            EventTracker.postEvent(ZetaConnectionFailedEvent(userName, noteId, interpreterClassName,
                    String.format("Failed to open interpreter for user: %s of note: %s with proxy user: %s log: %s",
                            userName, noteId, proxyUser, e.getMessage()), zce.getRecordTime()).addProperties(prop));


            if (e instanceof InterpreterStoppedException) {
                throw new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION, INTP_SESSION_CONNECTION_ABORT, noteId, e);
            } else {
                throw new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION, INTP_SESSION_CONNECTION_ERROR, noteId, e);
            }
        } catch (LifeCycleException e) {
            //logger.info("Failed to open interpreter for user: {} of note: {} with proxy user: {} log: {}", userName, noteId, proxyUser, e.getMessage());
            EventTracker.postEvent(ZetaConnectionFailedEvent(userName, noteId, interpreterClassName,
                    String.format("Failed to open interpreter for user: %s of note: %s with proxy user: %s log: %s",
                            userName, noteId, proxyUser, e.getMessage()), zce.getRecordTime()).addProperties(prop));
            throw new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION, INTP_SESSION_RESTRICTED_ERROR, noteId, e);
        } catch (Exception e) {
            //logger.info("Failed to open interpreter for user: {} of note: {} with proxy user: {} log: {}", userName, noteId, proxyUser, e.getMessage());
            EventTracker.postEvent(ZetaConnectionFailedEvent(userName, noteId, interpreterClassName,
                    String.format("Failed to open interpreter for user: %s of note: %s with proxy user: %s log: %s",
                            userName, noteId, proxyUser, e.getMessage()), zce.getRecordTime()).addProperties(prop));
            throw new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION, INTP_SESSION_CONNECTION_ERROR, noteId, e);
        }
    }

    private ZetaResponse<InterpreterRsp> openJDBC(String userName,
                                          String noteId,
                                          Properties prop) {
        // Properties prop = confManager.getConfiguration(userName, noteId, params);
        JdbcConf conf = (JdbcConf) prop.get(JDBC_CONF_KEY);
        return openNote(userName, conf.getJdbcType().interpreterType.getName(), noteId, prop);
    }


    private ZetaResponse<InterpreterRsp> openHDFS(String nt,
                                          String noteId,
                                          Properties params) {
        String password = params.getProperty("password");
        String host = params.getProperty("host", "");
        String hadoopConfigDir = params.getProperty("hadoopConfigDir", "");
        int clusterId = Integer.valueOf(params.getProperty("clusterId", "10"));
        Properties prop = new Properties();
        prop.setProperty(HDFSInterpreter.USERNAME_KEY, nt);
        prop.setProperty(HDFSInterpreter.PASSWORD_KEY, password);
        prop.setProperty(HDFSInterpreter.HOSTNAME_KEY, StringUtils.isNotBlank(host) ? host : "localhost");
        prop.setProperty(HDFSInterpreter.HADOOP_CONF_DIR_KEY, StringUtils.isNotBlank(hadoopConfigDir) ?
                hadoopConfigDir : ClusterUtil.getHadoopConfDir(clusterId));

        return openNote(nt, InterpreterType.EnumType.SSH_RESTRICTED_HDFS.getName(), noteId, prop);
    }

    private ZetaResponse<InterpreterRsp> openNote(String nt,
                                                  String interpType,
                                                  String noteId,
                                                  Properties prop) {
        String interpreterClassName = InterpreterType.toClass(interpType).getName();
        ZetaEvent zce = ZetaConnectionEvent(nt, noteId, interpreterClassName).addProperties(prop);
        EventTracker.postEvent(zce);
        try {
            openInterpreter(nt, noteId, interpreterClassName, prop);
        } catch (Exception ex) {
            EventTracker.postEvent(ZetaConnectionFailedEvent(nt, noteId, interpreterClassName, ex.toString(), zce.getRecordTime()).addProperties(prop));
            throw ex;
        }
        EventTracker.postEvent(ZetaConnectionSuccessEvent(nt, noteId, interpreterClassName, zce.getRecordTime()).addProperties(prop));
        return new ZetaResponse<>(new InterpreterRsp(INTP_SESSION_OPENED, "Note: " + noteId + " is opened"), HttpStatus.OK);
    }

    public String cancelNote(String userName, String noteId) {
        return manager.cancelNote(userName, noteId);
    }

    public String getAllInterpreterInfos() {
        return manager.getAllInterpreterInfos();
    }

    public String getInterpreterInfos(String userName) {
        return manager.getInterpreterInfos(userName);
    }

    public String getAllLifecycleInfos() {
        return manager.getAllLifecycleInfos();
    }

    public String getLifecycleInfos(String userName) {
        return manager.getLifecycleInfos(userName);
    }

    public String getUserFilterIntroduce(String userName) {
        return manager.getUserFilterIntroduce(userName);
    }

    public String removeLifecycle(String userName, String noteId) {
        return manager.removeLifecycle(userName, noteId);
    }

    public String kickUser(String userName, String noteId) {
        if (noteId == null) {
            return manager.removeContext(userName);
        } else {
            return manager.removeNote(userName, noteId);
        }
    }

    public String kickOnetimeNote(String userName) {
        return manager.clearOnetimeNote(userName);
    }

    public String grepConnectionStatus(String nt, String noteId) {
      return this.manager.grepConnectionStatus(nt, noteId);
    }

    public Interpreter findInterpreter(String noteId, String nt, String className) throws InterpreterException {
        return this.manager.getFactory().findInterpreter(nt, noteId, className);
    }

    public InterpreterManager getInterpreterManager() {
        return this.manager;
    }

    public ConfigurationManager getConfManager() {
        return confManager;
    }
}
