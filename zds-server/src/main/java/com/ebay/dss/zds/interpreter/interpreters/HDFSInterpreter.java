package com.ebay.dss.zds.interpreter.interpreters;

import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.output.result.Result;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zeppelin.interpreter.InterpreterResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.zeppelin.interpreter.InterpreterResult.Code.ERROR;
import static org.apache.zeppelin.interpreter.InterpreterResult.Code.SUCCESS;

public class HDFSInterpreter extends Interpreter {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String USERNAME_KEY = "zds.interpreter.hdfs.username";
    public static final String PASSWORD_KEY = "zds.interpreter.hdfs.password";
    public static final String HOSTNAME_KEY = "zds.interpreter.hdfs.hostname";
    public static final String HADOOP_CONF_DIR_KEY = "zds.interpreter.hdfs.configdir";

    private Session session;
    private Map<String, String> env;

    public HDFSInterpreter(Properties prop) throws JSchException, IOException {
        super(prop);
        String hostname = prop.getProperty(HOSTNAME_KEY);
        String username = prop.getProperty(USERNAME_KEY);
        String password = prop.getProperty(PASSWORD_KEY);
        String configDir = prop.getProperty(HADOOP_CONF_DIR_KEY);
        this.env = Collections.singletonMap("HADOOP_CONF_DIR", configDir);
        this.session = prepareSession(hostname, username, password);
        initKerberosTicket(session, password);
    }

    public HDFSInterpreter(Properties prop, Session session) {
        super(prop);
        String configDir = prop.getProperty(HADOOP_CONF_DIR_KEY);
        this.env = Collections.singletonMap("HADOOP_CONF_DIR", configDir);
        this.session = session;
    }

    void initKerberosTicket(Session session, String password) throws JSchException, IOException {
        // Get Kerberos ticket && refresh it.
        String cmd = "echo '" + password + "'|kinit && kinit -R";
//        String cmd = "echo Hello";
        String res = execCommand(cmd, session, null);
        LOGGER.info(res);
    }

    String execCommand(String cmd, Session session, Map<String, String> env) throws JSchException, IOException {
        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        if (Objects.nonNull(env)) {
            // set up environment
            env.keySet().stream()
                    .filter(Objects::nonNull)
                    .filter(key -> Objects.nonNull(env.get(key)))
                    .forEach(key -> channelExec.setEnv(key, env.get(key)));
        }
        channelExec.setCommand(cmd);
        channelExec.connect();
        Map.Entry<String, String> resOrError = getInputStreamAsText(channelExec);
        int exitStatus = channelExec.getExitStatus();
        if (exitStatus != 0) {
            LOGGER.error(resOrError.getValue());
            throw new JSchException("error on cmd " + cmd + " , exit: " + exitStatus);
        }
        return resOrError.getKey();
    }

    static Map.Entry<String, String> getInputStreamAsText(ChannelExec channelExec) throws IOException {
        InputStream in = channelExec.getInputStream();
        InputStream errIn = channelExec.getErrStream();
        StringBuilder cmdResultBuf = new StringBuilder();
        StringBuilder cmdErrorBuf = new StringBuilder();
        byte[] buf = new byte[1024];
        while (true) {
            cmdResultBuf = append(in, cmdResultBuf, buf);
            cmdErrorBuf = append(errIn, cmdErrorBuf, buf);
            if (channelExec.isClosed()) {
                break;
            }
            // Rarely reach here if cmd responses soon
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        return new AbstractMap.SimpleImmutableEntry<>(cmdResultBuf.toString(), cmdErrorBuf.toString());
    }

    static StringBuilder append(InputStream in, StringBuilder builder, byte[] buf) throws IOException {
        while (in.available() > 0) {
            int real = in.read(buf, 0, buf.length);
            if (real > 0) {
                builder.append(new String(buf, 0, real));
            } else {
                break;
            }
        }
        return builder;
    }

    Session prepareSession(String hostname, String username) throws JSchException {
        JSch jSch = new JSch();
        return jSch.getSession(username, hostname);
    }

    Session prepareSession(String hostname, String username, String password) throws JSchException {
        Session session = prepareSession(hostname, username);
        session.setPassword(password);
        // Avoid host checking.
        session.setConfig("StrictHostKeyChecking", "no");
        try {
            session.connect(30000);
        } catch (JSchException e) {
            LOGGER.error(e);
            throw e;
        }
        return session;
    }

    @Override
    public boolean isOpened() {
        return session.isConnected();
    }

    @Override
    protected void construct() throws Exception {

    }

    @Override
    public void close() {
        if (session.isConnected()) {
            session.disconnect();
            LOGGER.info("ssh restricted hdfs session is disconnected.");
        }
    }

    static String validCmd(String code) {
        // split by & | ; and their combinations such as && |& || and so on.
        LOGGER.info("Origin full cmd: " + code);
        Pattern pattern = Pattern.compile("((?:[^;&|\"']|\"[^\"]*\"|'[^']*')+)");
        Matcher matcher = pattern.matcher(code);
        List<String> splitCmdList = new ArrayList<>();
        while (matcher.find()) {
            String oneCmd = matcher.group().trim();
            splitCmdList.add(oneCmd);
            LOGGER.info("One cmd: " + oneCmd);
        }
        LOGGER.info("Origin cmd number: " + splitCmdList.size());
        // match hdfs
        List<String> startWithHdfs = splitCmdList.stream()
                .filter(cmd -> cmd.startsWith("hdfs"))
                .collect(Collectors.toList());
        LOGGER.info("Valid cmd number: " + startWithHdfs.size());
        // rejoin
        return StringUtils.join(startWithHdfs, " && ");
    }

    @Override
    protected InterpreterResult interpret(String code, String paragraphId) {
        String cmd = validCmd(code);
        try {
            String cmdRes = execCommand(cmd, session, env);
            return new InterpreterResult(SUCCESS, cmdRes);
        } catch (JSchException | IOException e) {
            LOGGER.error(e);
            return new InterpreterResult(ERROR, e.getMessage());
        }
    }

    @Override
    protected InterpreterResult interpret(String code, String paragraphId, InterpreterListener listener) {
        return interpret(code, paragraphId);
    }

    @Override
    public Result execute(String st, ExecutionContext context) {
        return null;
    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext executionContext) {
        return interpret(executionContext.getCode(), executionContext.getParagraphId().toString());
    }

    @Override
    protected InterpreterResult doExecute(ExecutionContext executionContext, InterpreterListener listener) {
        return interpret(executionContext.getCode(), executionContext.getParagraphId().toString(), listener);
    }

    @Override
    public void cancel(ExecutionContext context) {
        // not support yet
    }

    @Override
    public List<Integer> cancelAll() {
        // not support yet
        return Collections.emptyList();
    }

    @Override
    public FormType getFormType() {
        // diff native, simple and none???
        return FormType.NATIVE;
    }

    @Override
    public int getProgress(ExecutionContext context) {
        return 0;
    }

    @Override
    public boolean getPauseFlag() {
        return false;
    }

    @Override
    public void setPauseFlag(boolean flag) {

    }

    @Override
    public void doGc() {

    }
}

