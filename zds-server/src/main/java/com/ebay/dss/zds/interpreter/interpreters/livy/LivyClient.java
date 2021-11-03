package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.ebay.dss.zds.common.Constant;
import com.ebay.dss.zds.exception.SessionDeadException;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyMessage.*;
import com.ebay.dss.zds.interpreter.listener.ProcessListener;
import com.ebay.dss.zds.interpreter.monitor.modle.JobReport;
import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import com.ebay.dss.zds.transport.file.http.HttpUploadClient;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.livy.APINotFoundException;
import org.apache.zeppelin.livy.LivyException;
import org.apache.zeppelin.livy.SessionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.kerberos.client.KerberosRestTemplate;
import org.springframework.web.client.*;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2018/4/20.
 */
public class LivyClient {

    public static final String SESSION_NOT_FOUND_PATTERN = "\"Session '\\d+' not found.\"";
    // private static Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final String VALID_RESPONSE_MESSAGE = "CreateInteractiveRequest[\\\"master\\\"]";
    public static final String SESSION_DEAD_MESSAGE = "Session is in state dead";
    public static final String RPC_CHANNEL_CLOSE = "java.lang.IllegalStateException: RPC channel is closed.";
    public static final String SESSION_NOT_ACTIVE = "Session isn't active.";
    public static final String LIVY_CONF_HEADER = "zds.livy.";
    public static final String DUMP_LIMIT = Constant.DUMP_LIMIT;
    public static final String LIVY_CONF_SESSION_KIND = LIVY_CONF_HEADER + "session.kind";
    public static final String LIVY_CONF_SESSION_KIND_DEFAULT_VALUE = "spark";
    public static final String LIVY_CONF_URL = LIVY_CONF_HEADER + "url";
    public static final String LIVY_CONF_MAX_RESULT = LIVY_CONF_HEADER + "spark.result.dump.maxResult";
    public static final String LIVY_CONF_MAX_RESULT_DEFAULT_VALUE = "100000000";
    public static final String LIVY_CONF_AUTH_WHITELIST = LIVY_CONF_HEADER + "authenticate.whitelist";
    public static final String LIVY_CONF_AUTH_WHITELIST_DEFAULT_VALUE = "";
    public static final String LIVY_AUTHENTICATION_ENABLED = LIVY_CONF_HEADER + "authentication.enabled";
    public static final String LIVY_MAX_LOG_SIZE = LIVY_CONF_HEADER + "maxLogLines";
    private static final Logger LOGGER = LoggerFactory.getLogger(LivyClient.class);
    private Properties prop;
    private volatile String livyURL;
    private LivyURLSelector selector;
    private String sessionKind;
    private int maxDumpResult;
    private int maxLogLines;
    private boolean authenticationEnabled;
    private RestTemplate restTemplate;
    private CloseableHttpClient httpClient;

    public LivyClient(Properties property, String sessionKind) {
        this.sessionKind = sessionKind;
        init(property);
    }

    public LivyClient(Properties property) {
        this.sessionKind = property.getProperty(LIVY_CONF_SESSION_KIND, LIVY_CONF_SESSION_KIND_DEFAULT_VALUE);
        init(property);
    }

    public LivyClient() {
    }

    public static InterpreterResult getResultFromStatementInfo(
            SessionInfo sessionInfo,
            StatementInfo stmtInfo,
            boolean displayAppInfo) {

        if (stmtInfo.output != null && stmtInfo.output.isError()) {
            String traceback = stmtInfo.output.traceback == null ? "" : stmtInfo.output.traceback.toString();
            String trackMsg = StringUtils.isEmpty(traceback) ? "" : " traceback: " + traceback;
            String msg = (stmtInfo.output.evalue == null ? "" : stmtInfo.output.evalue) + trackMsg;
            if (StringUtils.isEmpty(msg))
                msg = "Unable to get the result of this statement, maybe the the statement was failed\n" +
                        "Please check the spark url for details: " + sessionInfo.webUIAddress;
            return new InterpreterResultBuilder().error().because(msg);
        } else if (stmtInfo.isCancelled()) {
            // corner case, output might be null if it is cancelled.
            return new InterpreterResultBuilder().error().cancelled();
        } else if (stmtInfo.output == null) {
            // This case should never happen, just in case
            return new InterpreterResultBuilder().error().emptyResult();
        } else {
            //TODO(zjffdu) support other types of data (like json, image and etc)
            /**This will get the text content from plain_text(first) or application/json(second)**/
            if (stmtInfo.output.data == null) {
                String msg = "Unable to get the result of this statement, maybe the the statement was failed\n" +
                        "Please check the spark url for details: " + sessionInfo.webUIAddress;
                return new InterpreterResultBuilder().error().because(msg);
            }
            String result = stmtInfo.output.data.getTextContent();
            // check table magic result first
            if (stmtInfo.output.data.application_livy_table_json != null) {
                List<String> headers = stmtInfo.output
                        .data
                        .application_livy_table_json
                        .headers
                        .stream()
                        .map(m -> m.get("name").toString())
                        .collect(Collectors.toList());
                List<List<Object>> records = new ArrayList<>();
                List<List> recordsTmp = stmtInfo.output
                        .data
                        .application_livy_table_json
                        .records;
                if (Objects.nonNull(recordsTmp)) {
                    recordsTmp.forEach(rtmp -> {
                        records.add((List<Object>) rtmp);
                    });
                }
                return new InterpreterResultBuilder()
                        .success()
                        .table(headers, records)
                        .build();
            } else if (stmtInfo.output.data.image_png != null) {
                return new InterpreterResultBuilder()
                        .success()
                        .image(stmtInfo.output.data.image_png)
                        .build();
            } else if (result != null) {
                result = result.trim();
                if (result.startsWith("<link")
                        || result.startsWith("<script")
                        || result.startsWith("<style")
                        || result.startsWith("<div")) {
                    result = "%html " + result;
                }
            }

            InterpreterResultBuilder builder = new InterpreterResultBuilder()
                    .success()
                    .content(result);
            if (displayAppInfo) {
                String appInfoHtml = "<hr/>Spark Application Id: " + sessionInfo.appId + "<br/>"
                        + "Spark WebUI: <a href=\"" + sessionInfo.webUIAddress + "\">"
                        + sessionInfo.webUIAddress + "</a>";
                return builder.html(appInfoHtml)
                        .build();
            } else {
                return builder.build();
            }
        }
    }

    public static boolean isDagUnfinished(String dag) {
        if (StringUtils.isEmpty(dag)) {
            LOGGER.warn("The dag is empty, just skip this dag");
            return false;
        } else return (dag.contains(DAGStatus.PENDING) || dag.contains(DAGStatus.RUNNING));
    }

    public static boolean isDagFinished(String dag) {
        return !isDagUnfinished(dag);
    }

    public boolean needAuthentication() {
        return authenticationEnabled && !inWhiteList(Arrays.asList(prop
                .getProperty(LIVY_CONF_AUTH_WHITELIST, LIVY_CONF_AUTH_WHITELIST_DEFAULT_VALUE)
                .split(",")), livyURL);
    }

    void init(Properties property) {
        this.prop = property;
        this.authenticationEnabled = Boolean.valueOf(property.getProperty(LIVY_AUTHENTICATION_ENABLED, "true"));
        this.selector = getLivyUrlSelector(property);
        checkAndSelectUrl();
        this.maxDumpResult = Integer.parseInt(property.getProperty(
                LIVY_CONF_MAX_RESULT, LIVY_CONF_MAX_RESULT_DEFAULT_VALUE));
        this.maxLogLines = Integer.parseInt(property.getProperty(LIVY_MAX_LOG_SIZE,
                "1000"));
        LOGGER.info("Livy client inited, url: [{}] need authentication: [{}]", livyURL, needAuthentication());
    }

    private void checkAndSelectUrl() {
        this.livyURL = this.selector.select();
        this.restTemplate = getRestTemplate(this.prop);
        while (!pingWith(this.livyURL)) {
            this.selector.removeUrl(this.livyURL);
            if (this.selector.hasNext()) {
                this.livyURL = this.selector.select();
                this.restTemplate = getRestTemplate(this.prop);
            } else break;
        }
    }

    private LivyURLSelector getLivyUrlSelector(Properties prop) {
        // todo: currently it should be: LivyURLNTHashSelector
        return LivyURLSelectorFactory.create(prop);
    }

    private boolean inWhiteList(List<String> whiteList, String url) {
        for (String allowUrl : whiteList) {
            if (allowUrl.contains(url) || url.contains(allowUrl)) {
                return true;
            }
        }
        return false;
    }

    public String getSessionKind() {
        return this.sessionKind;
    }

    public LivyURLSelector getURLSelector() {
        return this.selector;
    }

    public String getLivyURL() {
        return livyURL;
    }

    /** only for monitor purpose **/
    public boolean pingWith(String tmpUrl) {
        try {
            callRestAPI(tmpUrl, "/version", "GET", "", null);
            return true;
        } catch (Exception ex) {
            LOGGER.error(ex.toString(), ex);
            return false;
        }
    }

    public LivyMessage.SessionInfo createSession(LivyMessage.CreateSessionRequest request) throws LivyException {
        return LivyMessage.SessionInfo.fromJson(
                callRestAPI("/sessions", "POST", request.toJson()));
    }

    public SessionInfo getSessionInfo(int sessionId) throws LivyException {
        //logger.info("Get session info of session id: "+sessionId);
        return SessionInfo.fromJson(callRestAPI("/sessions/" + sessionId, "GET"));
    }

    public SessionLog getSessionLog(int sessionId) throws LivyException {
        return SessionLog.fromJson(callRestAPI("/sessions/" + sessionId + "/log?size=" + maxLogLines, "GET"));
    }

    public void closeSession(int sessionId) {
        try {
            callRestAPI("/sessions/" + sessionId, "DELETE");
        } catch (Exception e) {
            LOGGER.error(String.format("Error closing session for user with session ID: %s",
                    sessionId), e);
        }
    }

    public void closeSessionAsync(int sessionId) {
        new Thread(() -> closeSession(sessionId)).start();
    }

    public void moveQueue(int sessionId, MoveQueueRequest request) throws LivyException {
        callRestAPI("/sessions/" + sessionId + "/move", "POST", request.toJson());
    }

    public StatementInfo execute(int sessionId, ExecuteRequest executeRequest) throws LivyException {
        if (executeRequest instanceof ExecuteDumpRequest) {
            LOGGER.info("executing dump request, limit: " + ((ExecuteDumpRequest) executeRequest).limit);
            return executeDump(sessionId, (ExecuteDumpRequest) executeRequest);
        } else {
            return executeStatement(sessionId, executeRequest);
        }
    }

    public StatementInfo executeStatement(int sessionId, ExecuteRequest executeRequest)
            throws LivyException {
        return StatementInfo.fromJson(callRestAPI("/sessions/" + sessionId + "/statements", "POST",
                executeRequest.toJson()));
    }

    public StatementInfo executeDump(int sessionId, ExecuteDumpRequest dumpRequest)
            throws LivyException {
        if (StringUtils.isEmpty(dumpRequest.path)) {
            dumpRequest.path = "dummy_path";
        }
        return StatementInfo.fromJson(callRestAPI("/sessions/" + sessionId + "/dump", "POST",
                dumpRequest.toJson()));
    }

    public StatementInfo getStatementInfo(int sessionId, int statementId)
            throws LivyException {
        return StatementInfo.fromJson(callRestAPI("/sessions/" + sessionId + "/statements/" + statementId, "GET"));
    }

    public void cancelStatement(int sessionId, int statementId) throws LivyException {
        callRestAPI("/sessions/" + sessionId + "/statements/" + statementId + "/cancel", "POST");
    }

    public LivyVersion getLivyVersion() throws LivyException {
        return new LivyVersion((LivyVersionResponse.fromJson(callRestAPI("/version", "GET")).version));
    }

    public String getStatementDag(int sessionId, int statementId) throws LivyException {
        StatementInfo statementInfo = getStatementInfo(sessionId, statementId);
        return ((JobReport) statementInfo.getJobReport()).dag;
    }

    public FileSessionInfo createFileSession(CreateFileSessionRequest createFileSessionRequest) throws LivyException {
        return FileSessionInfo.fromJson(callRestAPI("/files", "POST", createFileSessionRequest.toJson()));
    }

    public void closeFileSession(int fileSessionId) {
        try {
            callRestAPI("/files/" + fileSessionId, "DELETE");
        } catch (Exception e) {
            LOGGER.error(String.format("Error closing session for user with session ID: %s",
                    fileSessionId), e);
        }
    }

    public PathCreateResponse createPath(int fileSessionId, PathCreateRequest pathCreateRequest) throws LivyException {
        return PathCreateResponse.fromJson(callRestAPI("/files/" + fileSessionId + "/create-path", "POST",
                pathCreateRequest.toJson()));
    }

    // to the fix file server in Livy side, chunk by chunk in on request. Choice for big file
    public FileUploadResponse uploadFile(int fileSessionId, FileUploadRequest fileUploadRequest) throws LivyException {
        return transferFile(fileSessionId,
                fileUploadRequest.port,
                fileUploadRequest.localFile,
                fileUploadRequest.dstFile,
                fileUploadRequest.overwrite);
    }

    // by the restemplate, one transfer per file, quick for small file
    public FileUploadResponse uploadFileV2(int fileSessionId, FileUploadRequestV2 fileUploadRequest) throws LivyException {
        return FileUploadResponse.fromJson(callRestAPI("/files/" + fileSessionId + "/upload-file/v2", "POST",
                fileUploadRequest.toJson()));
    }

    // by the restemplate, chunk by chunk, and one rest call per chunk
    public FileUploadResponse uploadFileV3(int fileSessionId, FileUploadRequestV3 fileUploadRequest) throws LivyException {
        return transferFileV3(
                fileUploadRequest.fileSessionId,
                fileUploadRequest.path,
                fileUploadRequest.inputStream,
                fileUploadRequest.bufferSize,
                fileUploadRequest.overwrite,
                fileUploadRequest.listener);
    }

    public FileGetResponse getFileV2(int fileSessionId, FileGetRequest fileGetRequest) throws LivyException {
        return FileGetResponse.fromJson(callRestAPI("/files/" + fileSessionId + "/get-file/v2", "POST",
                fileGetRequest.toJson()));
    }

    public FileCheckResponse checkExist(int fileSessionId, FileCheckRequest fileCheckRequest) throws LivyException {
        return FileCheckResponse.fromJson(callRestAPI("/files/" + fileSessionId + "/check-file", "POST",
                fileCheckRequest.toJson()));
    }

    public FolderListResponse folderList(int fileSessionId, FolderListRequest folderListRequest) throws LivyException {
        return FolderListResponse.fromJson(callRestAPI("/files/" + fileSessionId + "/folder-list", "POST",
                folderListRequest.toJson()));
    }

    public FileDeleteResponse deleteFile(int fileSessionId, FileDeleteRequest fileDeleteRequest) throws LivyException {
        return FileDeleteResponse.fromJson(callRestAPI("/files/" + fileSessionId + "/delete-file", "POST",
                fileDeleteRequest.toJson()));
    }

    public FileRenameResponse renameFile(int fileSessionId, FileRenameRequest fileRenameRequest) throws LivyException {
        return FileRenameResponse.fromJson(callRestAPI("/files/" + fileSessionId + "/rename-file", "POST",
                fileRenameRequest.toJson()));
    }

    public void addJar(int sessionId, AddResource addResource) throws LivyException {
        callRestAPI("/sessions/" + sessionId + "/add-jar", "POST", addResource.toJson());
    }

    public void addFile(int sessionId, AddResource addResource) throws LivyException {
        callRestAPI("/sessions/" + sessionId + "/add-file", "POST", addResource.toJson());
    }

    public void addPyFile(int sessionId, AddResource addResource) throws LivyException {
        callRestAPI("/sessions/" + sessionId + "/add-pyfile", "POST", addResource.toJson());
    }

    public <T> ResponseEntity<T> executeTemplate(String targetURL, HttpMethod method, HttpEntity<T> entity,
                                                 RequestCallback callback, Class<T> responseType) {
        if (callback == null) {
            return restTemplate.exchange(targetURL, method, entity, responseType);
        } else {
            throw new UnsupportedOperationException("Unsupported operation");
        }
    }

    public String callRestAPI(String targetURL, String method) throws LivyException {
        return callRestAPI(targetURL, method, "");
    }

    public String callRestAPI(String targetURL, String method, String jsonData)
            throws LivyException {
        return callRestAPI(targetURL, method, jsonData, null);
    }

    public String callRestAPI(String targetURL, String method, String jsonData, RequestCallback callback)
            throws LivyException {
        return callRestAPI(livyURL, targetURL, method, jsonData, callback);
    }

    public String callRestAPI(String baseUrl, String targetURL, String method, String jsonData, RequestCallback callback)
            throws LivyException {
        String paddedTargetURL = baseUrl + targetURL;
        LOGGER.debug("Call rest api in {}, method: {}, jsonData: {}", paddedTargetURL, method, jsonData);
        ResponseEntity<String> response = null;
        try {
            response = handleRequest(method, paddedTargetURL, jsonData, getHttpHeaders(), callback);
        } catch (HttpClientErrorException e) {
            response = new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
            LOGGER.error("Error with {} ZetaStatus: {}",
                    response.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            // Exception happens when kerberos is enabled.
            handleRestClientException(e);
        }

        if (response == null) {
            throw new LivyException("No http response returned");
        }

        return handleResponse(response, paddedTargetURL);
    }

    HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE);
        headers.add("X-Requested-By", "zds");
        headers.setConnection("close");
        return headers;
    }

    ResponseEntity<String> handleRequest(String method, String url, String jsonData, HttpHeaders headers, RequestCallback callback) {
        ResponseEntity<String> response = null;
        switch (method) {
            case "POST": {
                HttpEntity<String> entity = new HttpEntity<>(jsonData, headers);
                response = executeTemplate(url, HttpMethod.POST, entity, callback, String.class);
                break;
            }
            case "GET": {
                HttpEntity<String> entity = new HttpEntity<>(headers);
                response = executeTemplate(url, HttpMethod.GET, entity, callback, String.class);
                break;
            }
            case "DELETE": {
                HttpEntity<String> entity = new HttpEntity<>(headers);
                response = executeTemplate(url, HttpMethod.DELETE, entity, callback, String.class);
                break;
            }
        }
        return response;
    }

    String handleResponse(ResponseEntity<String> response, String url) throws LivyException {
        String responseString = response.getBody();
        HttpStatus status = response.getStatusCode();
        LOGGER.debug("Get response, ZetaStatus: {}, responseBody: {}", status, responseString);

        if (status.value() == 200
                || status.value() == 201) {
            //LOGGER.info(response.getBody());
            return responseString;
        } else if (status.value() == 404) {
            if (Objects.nonNull(responseString)
                    && responseString.matches(SESSION_NOT_FOUND_PATTERN)) {
                throw new SessionNotFoundException(responseString);
            } else {
                throw new APINotFoundException("No rest api found for " + url + ", " + status);
            }
        } else {
            if (StringUtils.contains(responseString, VALID_RESPONSE_MESSAGE)) {
                return responseString;
            }
            throw new LivyException(String.format("Error with %s ZetaStatus: %s", status.value(), responseString));
        }
    }

    void handleRestClientException(Throwable e) throws LivyException {
        if (e.getCause() instanceof HttpClientErrorException) {
            HttpClientErrorException cause = (HttpClientErrorException) e.getCause();
            if (cause.getResponseBodyAsString().matches(SESSION_NOT_FOUND_PATTERN)) {
                throw new SessionNotFoundException(cause.getResponseBodyAsString());
            }
            throw new LivyException(cause.getResponseBodyAsString() + "\n"
                    + ExceptionUtils.getFullStackTrace(ExceptionUtils.getRootCause(e)));
        }
        if (e instanceof HttpServerErrorException) {
            HttpServerErrorException errorException = (HttpServerErrorException) e;
            String errorResponse = errorException.getResponseBodyAsString();
            if (StringUtils.contains(errorResponse, SESSION_DEAD_MESSAGE) ||
                    StringUtils.contains(errorResponse, RPC_CHANNEL_CLOSE) ||
                    StringUtils.contains(errorResponse, SESSION_NOT_ACTIVE)) {
                throw new SessionDeadException();
            }
            throw new LivyException(errorResponse, e);
        }
        throw new LivyException(e);
    }

    public RestTemplate getRestTemplate(Properties properties) {
        String keytabLocation = properties.getProperty("zds.livy.keytab");
        String principal = properties.getProperty("zds.livy.principal");
        boolean isSpnegoEnabled = needAuthentication()
                && StringUtils.isNotEmpty(keytabLocation)
                && StringUtils.isNotEmpty(principal);

        if (livyURL.startsWith("https:")) {
            SSLConnectionSocketFactory csf = getSSLConnectionSocketFactory(properties);
            HttpClientBuilder httpClientBuilder = getHttpClientBuilder(csf);

            httpClientBuilder.setDefaultRequestConfig(getRequestConfig());
            httpClientBuilder.setDefaultCredentialsProvider(getCredentialsProvider());

            if (isSpnegoEnabled) {
                httpClientBuilder.setDefaultAuthSchemeRegistry(getAuthSchemeProviderRegistry());
            }

            httpClient = httpClientBuilder.build();
        }

        return getRestTemplate(keytabLocation, principal, isSpnegoEnabled, httpClient, properties);
    }

    HttpClientBuilder getHttpClientBuilder(SSLConnectionSocketFactory csf) {
        return HttpClients.custom().setSSLSocketFactory(csf);
    }

    Registry<AuthSchemeProvider> getAuthSchemeProviderRegistry() {
        return RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory())
                .build();
    }

    CredentialsProvider getCredentialsProvider() {
        Credentials credentials = new Credentials() {
            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public Principal getUserPrincipal() {
                return null;
            }
        };
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, credentials);
        return credsProvider;
    }

    RequestConfig getRequestConfig() {
        return new RequestConfig() {
            @Override
            public boolean isAuthenticationEnabled() {
                return true;
            }
        };
    }

    SSLConnectionSocketFactory getSSLConnectionSocketFactory(Properties properties) {
        String keystoreFile = properties.getProperty("zds.livy.ssl.trustStore");
        String password = properties.getProperty("zds.livy.ssl.trustStorePassword");
        checkKeyStoreAndPassword(keystoreFile, password);

        try (InputStream keystoreIn = getInputStreamOfKeystoreFile(keystoreFile)) {
            KeyStore trustStore = getKeyStore();
            trustStore.load(keystoreIn, password.toCharArray());
            return getSSLConnectionSocketFactory(trustStore);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSLConnectionSocketFactory", e);
        }
    }

    SSLConnectionSocketFactory getSSLConnectionSocketFactory(KeyStore keyStore) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(keyStore, new TrustAllStrategy())
                .build();
        return new SSLConnectionSocketFactory(sslContext);
    }

    KeyStore getKeyStore() throws KeyStoreException {
        return KeyStore.getInstance(KeyStore.getDefaultType());
    }

    InputStream getInputStreamOfKeystoreFile(String keystoreFile) throws FileNotFoundException {
        return new FileInputStream(keystoreFile);
    }

    void checkKeyStoreAndPassword(String keystoreFile, String password) {
        if (StringUtils.isBlank(keystoreFile)) {
            throw new RuntimeException("No zds.livy.ssl.trustStore specified for livy ssl");
        }
        if (StringUtils.isBlank(password)) {
            throw new RuntimeException("No zds.livy.ssl.trustStorePassword specified " +
                    "for livy ssl");
        }
    }

    private static final String IMPERSONATE_KITE_KEY = "zds.livy.impersonate.kite";
    private static final String IMPERSONATE_PRINCIPAL_KEY = "zds.livy.impersonate.principal";
    private static final String IMPERSONATE_TICKET_CACHE_KEY = "zds.livy.impersonate.ticket-cache";
    private static final String IMPERSONATE_KITE_DEFAULT = "true";
    private static final String IMPERSONATE_PRINCIPAL_DEFAULT = "b_zeta_devsuite@PROD.EBAY.COM";
    private static final String IMPERSONATE_TICKET_CACHE_DEFAULT = "/tmp/krb5cc_b_zeta_devsuite";


    private Map<String, Object> getKiteLoginOptions(Properties properties) {
        Map<String, Object> options = new HashMap<>();
        options.put("refreshKrb5Config", "true");
        options.put("useTicketCache",  "true");
        options.put("ticketCache", properties.getProperty(IMPERSONATE_TICKET_CACHE_KEY, IMPERSONATE_TICKET_CACHE_DEFAULT));
        options.put("principal", properties.getProperty(IMPERSONATE_PRINCIPAL_KEY, IMPERSONATE_PRINCIPAL_DEFAULT));
        options.put("renewTGT",  "true");
        return options;
    }

    RestTemplate getRestTemplate(String principal, String keytabLocation, boolean isSpnegoEnabled, CloseableHttpClient client, Properties properties) {
        RestTemplate template;
        boolean isKiteEnabled = BooleanUtils.toBoolean(
                properties.getProperty(IMPERSONATE_KITE_KEY, IMPERSONATE_KITE_DEFAULT));
        if (isKiteEnabled) {
            LOGGER.info("Livy use Kite do authorization");
            Map<String, Object> options = getKiteLoginOptions(properties);
            LOGGER.info("Livy use following login options: {}", options);
            template = new KerberosRestTemplate(null, (String) options.get("principal"), options);
        } else if (client != null && isSpnegoEnabled) {
            LOGGER.info("Livy use Keytab with custom http client do authorization");
            template = new KerberosRestTemplate(keytabLocation, principal, client);
        } else if (isSpnegoEnabled) {
            LOGGER.info("Livy use Keytab do authorization");
            template = new KerberosRestTemplate(keytabLocation, principal);
        } else if (client == null) {
            template = new RestTemplate();
        } else {
            template = new RestTemplate(new HttpComponentsClientHttpRequestFactory(client));
        }
        template.getMessageConverters().add(0,
                new StringHttpMessageConverter(Charset.forName("UTF-8")));
        return template;
    }

    public Boolean isAlive(int sessionId, int statementId) {
        Boolean flag = false;
        try {
            StatementInfo si = getStatementInfo(sessionId, statementId);
            flag = si.isRunning();
            LOGGER.info("Statement: {} of Session: {} State: {} ", statementId, sessionId, si.state);
        } catch (Exception ex) {
            LOGGER.info("Got exception while check cancel: " + ex.getMessage() + " assert that the statement is canceled");
            flag = false;
        }
        return flag;
    }

    public void close() {
        try {
            LOGGER.info("close livy client");
            if (httpClient != null) {
                httpClient.close();
                ClientConnectionManager cm = httpClient.getConnectionManager();
                if (cm != null) {
                    cm.closeIdleConnections(0, TimeUnit.SECONDS);
                    cm.shutdown();
                }
            }
            LOGGER.info("closed");
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Failed to close Livy client: " + e.getMessage());
        }
    }

    public ClientConnectionManager getConnectionManager() {
        if (httpClient != null) {
            return this.httpClient.getConnectionManager();
        } else return null;
    }

    public String withNewLocator(int port) {
        return new LivyURL(livyURL).withNewLocatorPattern(port);
    }

    public FileUploadResponse transferFile(int fileSessionId,
                                           int port,
                                           String localFile,
                                           String dstPath,
                                           boolean overwrite) throws LivyException {
        Properties prop = new Properties();
        prop.put("base_url", withNewLocator(port));
        prop.put("file_session_id", String.valueOf(fileSessionId));
        prop.put("src_file", localFile);
        prop.put("dst_file", dstPath);
        prop.put("overwrite", String.valueOf(overwrite));
        HttpUploadClient httpUploadClient = new HttpUploadClient(prop);

        try {
            httpUploadClient.upload();
        } catch (Exception ex) {
            throw new LivyException(ex);
        }

        FileUploadResponse response = new FileUploadResponse();
        response.success = true;
        response.path = dstPath;
        return response;
    }

    public FileUploadResponse transferFileV3(
            int fileSessionId,
            String path,
            InputStream inputStream,
            int bufferSize,
            boolean overwrite,
            ProcessListener<MultiPartFileUploadResponse,
                    MultiPartFileChunkAck,
                    MultiPartFileUploadFinishAck> listener) throws LivyException {

        boolean shouldListen = true;
        if (listener == null) {
            shouldListen = false;
        }

        MultiPartFileUploadResponse multiPartFileUploadResponse =
                MultiPartFileUploadResponse.fromJson(
                        callRestAPI("/files/" + fileSessionId + "/upload-file/v3/open-process", "POST",
                                new MultiPartFileUploadRequest(path, overwrite).toJson()));
        long transferId = multiPartFileUploadResponse.transferId;

        if (shouldListen) listener.beforeProcess(multiPartFileUploadResponse);

        long total = 0L;
        byte[] buffer = new byte[bufferSize];
        try {
            int i = inputStream.read(buffer);
            while (i != -1) {
                total += i;
                byte[] chunk = new byte[i];
                System.arraycopy(buffer, 0, chunk, 0, i);
                // upload
                MultiPartFileChunkAck ack = MultiPartFileChunkAck
                        .fromJson(callRestAPI("/files/" + fileSessionId + "/upload-file/v3/chunk", "POST",
                                new MultiPartFileChunk(transferId, chunk).toJson()));
                if (shouldListen) listener.onProcess(ack);
                i = inputStream.read(buffer);
            }
        } catch (Exception e) {
            throw new LivyException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        MultiPartFileUploadFinishAck finishAck =
                MultiPartFileUploadFinishAck.fromJson(callRestAPI("/files/" + fileSessionId + "/upload-file/v3/close-process", "POST",
                        new MultiPartFileUploadFinish(transferId).toJson()));
        if (shouldListen) listener.afterProcess(finishAck);

        FileUploadResponse response = new FileUploadResponse();
        if (total == finishAck.currentRead) {
            response.success = true;
            response.path = path;
        } else {
            response.success = false;
            response.error = String.format("Incomplete transfer, origin/uploaded: %s/%s",
                    total, finishAck.currentRead);
        }

        return response;
    }

    public static abstract class LivyResponse {
        public static final String FATAL_ERROR_PATTERN = "Fatal errors happened, please reconnect.";
        protected Pattern pattern;
        protected Matcher matcher;
        protected Boolean find = null;

        public LivyResponse(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }

        public boolean equals(String response) {
            Matcher matcher = pattern.matcher(response);
            find = matcher.find();
            if (find) {
                this.matcher = matcher;
            }
            return find;
        }

        public String tryExtract(String response) {
            try {
                if (find != null) {
                    if (find) {
                        return matcher.group();
                    } else return missed();
                } else {
                    return extractKeyWords(response);
                }
            } finally {
                find = null;
                matcher = null;
            }
        }

        protected String missed() {
            return "";
        }

        public abstract String extractKeyWords(String response);
    }

    public static class TimeoutResponse extends LivyResponse {

        public TimeoutResponse() {
            super("No YARN application is found with tag");
        }

        public String extractKeyWords(String response) {
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                return "Failed to apply resource from Hadoop cluster." +
                        " Please make sure the queue has enough resources, if not, please switch to another queue and then try again.";
            } else return missed();
        }

        @Override
        protected String missed() {
            return "";
        }
    }

    public static class UnknownQueueResponse extends LivyResponse {

        public UnknownQueueResponse() {
            super("Application application_.*submitted by user.*to unknown queue:[^,^\"]*");
        }

        public String extractKeyWords(String response) {
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                return matcher.group();
            } else return missed();
        }

        @Override
        protected String missed() {
            return "";
        }
    }

    public static class QueueAccessDeniedResponse extends LivyResponse {

        public QueueAccessDeniedResponse() {
            super("User.*cannot submit applications to queue.*?,");
        }

        public String extractKeyWords(String response) {
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                String group = matcher.group();
                return group.substring(0, group.length() - 2) +
                        ". Please go to https://bdp.corp.ebay.com/queue for applying access";
            } else return missed();
        }

        @Override
        protected String missed() {
            return "";
        }
    }

    public static class ClusterAccessDeniedResponse extends LivyResponse {

        public ClusterAccessDeniedResponse() {
            super("org.apache.hadoop.security.AccessControlException: Permission denied: user=.*, access=WRITE,");
        }

        public String extractKeyWords(String response) {
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                return matcher.group().substring(0, matcher.group().length() - 2) +
                        ". Please make sure you have the permission to access this cluster." +
                        " Check and request for cluster access: https://bdp.corp.ebay.com/cluster/request";
            } else return missed();
        }

        @Override
        protected String missed() {
            return "";
        }
    }

    public static class SparkDriverErrorResponse extends LivyResponse {

        public SparkDriverErrorResponse() {
            super("SparkDriver Error: .*?\",");
        }

        public String extractKeyWords(String response) {
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                String group = matcher.group();
                return group.substring(0, group.length() - 2).replace("SparkDriver Error: ", "");
            } else return missed();
        }

        @Override
        protected String missed() {
            return "";
        }
    }

    public static class SomeExceptionResponse extends LivyResponse {

        public SomeExceptionResponse() {
            super("Caused by:.*?,");
        }

        public String extractKeyWords(String response) {
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                return matcher.group().substring(0, matcher.group().length() - 2);
            } else return missed();
        }

        @Override
        protected String missed() {
            return "";
        }
    }

    public static class UnknownExceptionResponse extends LivyResponse {

        public UnknownExceptionResponse() {
            super(".*");
        }

        public String extractKeyWords(String response) {
            /*
            return "Failed to connect. Please do: 1) make sure the queue has enough resources, if not, please switch to another queue and then try again." +
                    " 2) verify the configuration, bad parameters will shutdown the connection";*/
            return response;
        }
    }

    public static class GSSInitialFailedResponse extends LivyResponse {
        public GSSInitialFailedResponse() {
            super("Could not connect to meta store using any of the URIs provided.*?GSS initiate failed");
        }

        public String extractKeyWords(String response) {
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                return "Fatal errors happened, please reconnect. reason: couldn't connect to HiveMetastore by: " + matcher.group();
            } else return response;
        }

        @Override
        public String tryExtract(String response) {
            try {
                if (find != null) {
                    if (find) {
                        return "Fatal errors happened, please reconnect. reason: couldn't connect to HiveMetastore by: " + matcher.group();
                    } else return response;
                } else {
                    return extractKeyWords(response);
                }
            } finally {
                find = null;
                matcher = null;
            }
        }
    }

    public static class ErrorAcquiringPasswordResponse extends LivyResponse {
        public ErrorAcquiringPasswordResponse() {
            super("Could not connect to meta store using any of the URIs provided.*?" +
                    "DIGEST-MD5: IO error acquiring password");
        }

        public String extractKeyWords(String response) {
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                return FATAL_ERROR_PATTERN + " reason: couldn't connect to HiveMetastore by: " + matcher.group();
            } else return response;
        }

        @Override
        public String tryExtract(String response) {
            try {
                if (find != null) {
                    if (find) {
                        return FATAL_ERROR_PATTERN + " reason: couldn't connect to HiveMetastore by: " + matcher.group();
                    } else return response;
                } else {
                    return extractKeyWords(response);
                }
            } finally {
                find = null;
                matcher = null;
            }
        }
    }

    public static class DAGStatus {
        public static final String RUNNING = "RUNNING";
        public static final String PENDING = "PENDING";
    }
}
