package com.ebay.dss.zds.model;

import com.ebay.dss.zds.common.JsonUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import parquet.org.codehaus.jackson.annotate.JsonWriteNullProperties;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

import java.util.Map;
import java.util.Optional;

/**
 * Created by tatian on 2019-11-28.
 */
public class ZetaNotebookPreference {

  @JsonProperty("notebook.connection")
  public Map<String, Object> notebookConnection;

  @JsonProperty("notebook.profile")
  public String notebookProfile;

  @JsonProperty("notebook.variables")
  public Map<String, String> variables;

  @JsonProperty("notebook.vargenerators")
  public Map<String, String> vargenerators;

  @JsonProperty("notebook.layout")
  public Map<String, String> notebookLayout;

  public static class NotebookConnection {
    public String alias;
    public String source;
    public String clusterId;
    public String batchAccount;
    public String codeType; // SQL, TERADATA
  }

  public String toJson() {
    return JsonUtil.toJson(this);
  }

  public static ZetaNotebookPreference fromJson(String json) {
    return JsonUtil.fromJson(json, ZetaNotebookPreference.class);
  }

  public static Optional<Object> getPlatformAlias(@NotNull Map<String, Object> notebookConnection) {
    return getValue("alias", notebookConnection);
  }

  public static Optional<Object> getBatchAccount(@NotNull Map<String, Object> notebookConnection) {
    return getValue("batchAccount", notebookConnection);
  }

  public static Optional<Object> getCodeType(@NotNull Map<String, Object> notebookConnection) {
    return getValue("codeType", notebookConnection);
  }

  public static Optional<Object> getPlatformIdentifier(@NotNull Map<String, Object> notebookConnection) {
    Optional<Object> alias = getPlatformAlias(notebookConnection);
    Optional<Object> codeType = getCodeType(notebookConnection);
    return (alias.isPresent() && codeType.isPresent()) ? Optional.of(alias.get() + "#" + codeType.get()) : Optional.empty();
  }

  private static Optional<Object> getValue(String key, @NotNull Map<String, Object> notebookConnection) {
    return Optional.ofNullable(notebookConnection.get(key));
  }

  public Map<String, Object> cloneNotebookConnection() {
    if (this.notebookConnection != null) {
      return new HashMap<>(this.notebookConnection);
    } else {
      return null;
    }
  }

  public static ZetaNotebookPreference getInstance(Map<String, Object> notebookConnection) {
    ZetaNotebookPreference preference = new ZetaNotebookPreference();
    preference.notebookConnection = notebookConnection;
    return preference;
  }

}
