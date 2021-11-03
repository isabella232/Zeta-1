package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.ebay.dss.zds.interpreter.listener.ProcessListener;
import com.ebay.dss.zds.interpreter.monitor.modle.JobReport;
import com.ebay.dss.zds.interpreter.monitor.modle.Status;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by tatian on 2019-10-16.
 */
public class LivyMessage {

  private static Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  public static class CreateSessionRequest {
    public final String kind;
    @SerializedName("proxyUser")
    public final String user;
    public final Map<String, String> conf;

    public CreateSessionRequest(String kind, String user, Map<String, String> conf) {
      this.kind = kind;
      this.user = user;
      this.conf = conf;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class MoveQueueRequest {
    public final String queue;

    public MoveQueueRequest(String queue) {
      this.queue = queue;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class SessionInfo {

    public final int id;
    public String appId;
    public String webUIAddress;
    public final String owner;
    public final String proxyUser;
    public final String state;
    public final String kind;
    public final Map<String, String> appInfo;
    public final List<String> log;

    public SessionInfo(int id, String appId, String owner, String proxyUser, String state,
                       String kind, Map<String, String> appInfo, List<String> log) {
      this.id = id;
      this.appId = appId;
      this.owner = owner;
      this.proxyUser = proxyUser;
      this.state = state;
      this.kind = kind;
      this.appInfo = appInfo;
      this.log = log;
    }

    public boolean isReady() {
      return state.equals("idle");
    }

    public boolean isFinished() {
      return state.equals("error") ||
              state.equals("dead") ||
              state.equals("success") ||
              state.equals("shutting_down");
    }

    public String toJson() {
      return gson.toJson(this);
    }

    public static SessionInfo fromJson(String json) {
      return gson.fromJson(json, SessionInfo.class);
    }
  }

  public static class SessionLog {
    public int id;
    public int from;
    public int size;
    public List<String> log;

    SessionLog() {
    }

    public static SessionLog fromJson(String json) {
      return gson.fromJson(json, SessionLog.class);
    }

    public String asString() {
      return StringUtils.join(log, "");
    }
  }

  public static class ExecuteRequest {
    public final String code;
    public final String kind;

    public ExecuteRequest(String code, String kind) {
      this.code = code;
      this.kind = kind;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class ExecuteDumpRequest extends ExecuteRequest {
    public final int limit;
    public String path;

    public ExecuteDumpRequest(String code, String kind, String path, int limit) {
      super(code, kind);
      this.limit = limit;
      this.path = path;
    }

    public ExecuteDumpRequest(String code, String kind, int limit) {
      this(code, kind, null, limit);
    }

    @Override
    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class ProgressInfo {
    public JobReport jobReport;

    public ProgressInfo() {
    }
  }

  public static class StatementInfo {
    public Integer id;
    public String state;
    public StatementOutput output;
    public double progress;
    public ProgressInfo progressInfo;

    public StatementInfo() {
    }

    public static StatementInfo fromJson(String json) {
      return gson.fromJson(json, StatementInfo.class);
    }

    public boolean isAvailable() {
      return state.equals("available") || state.equals("cancelled");
    }

    public boolean isCancelled() {
      return state.equals("cancelled");
    }

    public boolean isCancelling() {
      return state.equals("cancelling");
    }

    public boolean isWaiting() {
      return state.equals("waiting");
    }

    public boolean isRunning() {
      return state.equals("running");
    }

    public boolean isError() {
      return state.equals("error");
    }

    @NotNull
    public Status getJobReport() {
      if (progressInfo != null && progressInfo.jobReport != null) {
        return progressInfo.jobReport.setProgress(progress);
      } else {
        if (output == null || output.jobReport == null) {
          JobReport report = new JobReport();
          report.statementId = id;
          if (progress != 1.0) {
            report.status = Status.StatusEnum.WAITING.getName();
          } else {
            report.status = Status.StatusEnum.SUCCESS.getName();
          }
          return report.setProgress(progress);
        } else return output.jobReport.setProgress(progress);
      }
    }

    public static class StatementOutput {
      public String status;
      public String execution_count;
      public Data data;
      public String ename;
      public String evalue;
      public Object traceback;
      public TableMagic tableMagic;
      public JobReport jobReport;

      public boolean isError() {
        return Optional.ofNullable(status).orElse("error").equals("error");
      }

      public String toJson() {
        return gson.toJson(this);
      }

      public static class Data {
        @SerializedName("text/plain")
        public String plain_text;
        @SerializedName("image/png")
        public String image_png;
        @SerializedName("application/json")
        public JsonObject application_json;
        @SerializedName("application/vnd.livy.table.v1+json")
        public TableMagic application_livy_table_json;

        public String getTextContent() {
          if (plain_text != null) {
            return plain_text;
          } else if (application_json != null) {
            return application_json.toString();
          } else {
            return plain_text;
          }
        }
      }

      public static class TableMagic {
        @SerializedName("headers")
        List<Map> headers;

        @SerializedName("data")
        List<List> records;
      }

    }
  }

  public static class LivyVersionResponse {
    public String url;
    public String branch;
    public String revision;
    public String version;
    public String date;
    public String user;

    public static LivyVersionResponse fromJson(String json) {
      return gson.fromJson(json, LivyVersionResponse.class);
    }
  }

  public static class CreateFileSessionRequest {
    @SerializedName("proxyUser")
    public final String user;

    public CreateFileSessionRequest(String user) {
      this.user = user;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class FileSessionInfo {
    public int id;
    public String owner;
    public String proxyUser;
    public int fileTransferPort;

    public String toJson() {
      return gson.toJson(this);
    }

    public static FileSessionInfo fromJson(String json) {
      return gson.fromJson(json, FileSessionInfo.class);
    }
  }

  /** path create**/
  public static class PathCreateRequest {
    public String path;
    public Short permission; // (short)00777

    public PathCreateRequest(String path) {
      this.path = path;
      this.permission = -1;
    }

    public PathCreateRequest(String path, Short permission) {
      this.path = path;
      this.permission = permission;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class PathCreateResponse {
    public boolean success;
    public String path;
    public String error;

    public String toJson() {
      return gson.toJson(this);
    }

    public static PathCreateResponse fromJson(String json) {
      return gson.fromJson(json, PathCreateResponse.class);
    }
  }

  /** upload v1**/
  public static class FileUploadRequest {
    public int port;
    public String localFile;
    public String dstFile;
    boolean overwrite;

    public FileUploadRequest(int port, String localFile, String dstFile, boolean overwrite) {
      this.port = port;
      this.localFile = localFile;
      this.dstFile = dstFile;
      this.overwrite = overwrite;
    }

    public FileUploadRequest(int port, File localFile, String dstFile, boolean overwrite) {
      this.port = port;
      this.localFile = localFile.getAbsolutePath();
      this.dstFile = dstFile;
      this.overwrite = overwrite;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  /** upload v2**/
  public static class FileUploadRequestV2 {
    public String file;
    public String path;
    boolean overwrite;

    public FileUploadRequestV2(byte[] file, String path, boolean overwrite) {
      this.file = Base64.getEncoder().encodeToString(file);
      this.path = path;
      this.overwrite = overwrite;
    }

    public FileUploadRequestV2(byte[] file, String path) {
      this.file = Base64.getEncoder().encodeToString(file);
      this.path = path;
      this.overwrite = true;
    }

    public FileUploadRequestV2(File file, String path) throws IOException {
      this.file = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(file));
      this.path = path;
      this.overwrite = true;
    }

    public FileUploadRequestV2(File file, String path, boolean overwrite) throws IOException {
      this.file = Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(file));
      this.path = path;
      this.overwrite = overwrite;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class FileUploadResponse {
    public boolean success;
    public String path;
    public String error;

    public String toJson() {
      return gson.toJson(this);
    }

    public static FileUploadResponse fromJson(String json) {
      return gson.fromJson(json, FileUploadResponse.class);
    }
  }

  /** upload v3**/

  public static class FileUploadRequestV3 {
    int fileSessionId;
    String path;
    InputStream inputStream;
    int bufferSize;
    boolean overwrite;
    ProcessListener<MultiPartFileUploadResponse,
            MultiPartFileChunkAck,
            MultiPartFileUploadFinishAck> listener;

    public FileUploadRequestV3(int fileSessionId,
                               String path,
                               InputStream inputStream,
                               int bufferSize,
                               boolean overwrite,
                               ProcessListener<MultiPartFileUploadResponse,
                                       MultiPartFileChunkAck,
                                       MultiPartFileUploadFinishAck> listener) {
     this.fileSessionId = fileSessionId;
     this.path = path;
     this.inputStream = inputStream;
     this.bufferSize = bufferSize;
     this.overwrite = overwrite;
     this.listener = listener;
    }

    public FileUploadRequestV3(int fileSessionId,
                               String path,
                               InputStream inputStream,
                               int bufferSize,
                               boolean overwrite) {
      this.fileSessionId = fileSessionId;
      this.path = path;
      this.inputStream = inputStream;
      this.bufferSize = bufferSize;
      this.overwrite = overwrite;
    }
    public String toJson() {
      return gson.toJson(this);
    }
  }
  // open file transfer process
  public static class MultiPartFileUploadRequest {
    public String path;
    boolean overwrite;

    public MultiPartFileUploadRequest(String path, boolean overwrite) {
      this.path = path;
      this.overwrite = overwrite;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class MultiPartFileUploadResponse {
    public long transferId;

    public String toJson() {
      return gson.toJson(this);
    }

    public static MultiPartFileUploadResponse fromJson(String json) {
      return gson.fromJson(json, MultiPartFileUploadResponse.class);
    }
  }

  // upload file chunk
  public static class MultiPartFileChunk {
    public long transferId;
    public String chunk;

    public MultiPartFileChunk(long transferId, byte[] file) {
      this.transferId = transferId;
      this.chunk = Base64.getEncoder().encodeToString(file);
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class MultiPartFileChunkAck {
    public long transferId;
    public long currentRead;

    public String toJson() {
      return gson.toJson(this);
    }

    public static MultiPartFileChunkAck fromJson(String json) {
      return gson.fromJson(json, MultiPartFileChunkAck.class);
    }
  }

  public static class MultiPartFileUploadFinish {
    public long transferId;

    public MultiPartFileUploadFinish(long transferId) {
      this.transferId = transferId;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class MultiPartFileUploadFinishAck {
    public long transferId;
    public long currentRead;

    public String toJson() {
      return gson.toJson(this);
    }

    public static MultiPartFileUploadFinishAck fromJson(String json) {
      return gson.fromJson(json, MultiPartFileUploadFinishAck.class);
    }
  }

  /** get **/
  public static class FileGetRequest {
    public String path;

    public FileGetRequest(String path) {
      this.path = path;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class FileGetResponse {
    public boolean success;
    public String file;
    public String error;

    public static FileGetResponse fromJson(String json) {
      return gson.fromJson(json, FileGetResponse.class);
    }

    public String toJson() {
      return gson.toJson(this);
    }

    public byte[] getAsByte() {
      if (file != null) {
        return Base64.getDecoder().decode(file);
      } else return null;
    }
  }

  /** check **/
  public static class FileCheckRequest {
    public String path;

    public FileCheckRequest(String path) {
      this.path = path;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class FileCheckResponse {
    public boolean exist;
    public String error;

    public String toJson() {
      return gson.toJson(this);
    }

    public static FileCheckResponse fromJson(String json) {
      return gson.fromJson(json, FileCheckResponse.class);
    }
  }

  /** list folder **/
  public static class FolderListRequest {
    public String path;
    public String suffix;

    public FolderListRequest(String path) {
      this.path = path;
      this.suffix = null;
    }

    public FolderListRequest(String path, String suffix) {
      this.path = path;
      this.suffix = suffix;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class FolderListResponse {
    public String path;
    public List<HadoopFileStatus> fileStatus;

    public static FolderListResponse fromJson(String json) {
      return gson.fromJson(json, FolderListResponse.class);
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class HadoopFileStatus {
    public String path;
    public String owner;
    public short permission;
    public long accessTime;
    public long modifyTime;
    public boolean isFile;

    public static HadoopFileStatus fromJson(String json) {
      return gson.fromJson(json, HadoopFileStatus.class);
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  /** delete **/
  public static class FileDeleteRequest {
    public String path;

    public FileDeleteRequest(String path) {
      this.path = path;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class FileDeleteResponse {
    public boolean deleted;
    public String error;

    public String toJson() {
      return gson.toJson(this);
    }

    public static FileDeleteResponse fromJson(String json) {
      return gson.fromJson(json, FileDeleteResponse.class);
    }
  }

  /** rename **/
  public static class FileRenameRequest {
    public String oldPath;
    public String newPath;

    public FileRenameRequest(String oldPath, String newPath) {
      this.oldPath = oldPath;
      this.newPath = newPath;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

  public static class FileRenameResponse {
    public boolean success;
    public String oldPath;
    public String newPath;
    public String error;

    public String toJson() {
      return gson.toJson(this);
    }

    public static FileRenameResponse fromJson(String json) {
      return gson.fromJson(json, FileRenameResponse.class);
    }
  }

  /** add resource **/
  public static class AddResource {

    public final String uri;

    public AddResource(String uri) {
      this.uri = uri;
    }

    public String toJson() {
      return gson.toJson(this);
    }
  }

}
