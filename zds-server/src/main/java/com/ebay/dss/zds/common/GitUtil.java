package com.ebay.dss.zds.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.google.common.base.Splitter;
import com.jcabi.github.Branch;
import com.jcabi.github.Commit;
import com.jcabi.github.Content;
import com.jcabi.github.Coordinates;
import com.jcabi.github.FromProperties;
import com.jcabi.github.Github;
import com.jcabi.github.Reference;
import com.jcabi.github.References;
import com.jcabi.github.Repo;
import com.jcabi.github.RtGithub;
import com.jcabi.github.Tag;
import com.jcabi.http.Request;
import com.jcabi.http.request.ApacheRequest;
import com.jcabi.http.response.JsonResponse;
import com.jcabi.http.wire.AutoRedirectingWire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Created by zhouhuang on 2018年12月6日
 */
@Component
public class GitUtil {

  private final static Logger logger = LoggerFactory.getLogger(GitUtil.class);

  private final static String PATTERN_1 = "https://([^/:]+)/([^#/ ]*)/([^#/\\. ]*)";
  private final static String PATTERN_2 = "git@([^/:]+):([^# ]*)/([^#\\. ]*)";

  @Resource(name = "error-handle-rest-template")
  private RestTemplate restTemplate;

  public List<String> parseGitURL(String url) {
    logger.info("Parse Git URL: {}", url);
    if (StringUtils.isBlank(url)) {
      throw new ToolSetCheckException("Github url is empty!");
    }
    List gitInfo = Lists.newArrayList();
    if (url.toLowerCase().trim().startsWith("https")) {
      parseGitUrl(url, PATTERN_1, gitInfo);
    } else if (url.toLowerCase().trim().startsWith("git")) {
      parseGitUrl(url, PATTERN_2, gitInfo);
    }

    if (gitInfo.size() < 3) {
      throw new ToolSetCheckException("This is not a valid github url! " +
          "Valid Github url is like [https://github.corp.ebay.com/proj/repo] OR " +
          "[git@github.corp.ebay.com:proj/repo.git]");
    }

    return gitInfo;
  }

  private void parseGitUrl(String url, String pattern, List<String> gitInfo) {
    Pattern r = Pattern.compile(pattern);

    Matcher m = r.matcher(url);
    if (m.find()) {
      gitInfo.add(m.group(1));
      gitInfo.add(m.group(2));
      gitInfo.add(m.group(3));
    }
  }

  public Github getGit(String gitRepo, String token) {
    Request REQUEST = new ApacheRequest(gitRepo).header(HttpHeaders.USER_AGENT, new FromProperties("jcabigithub"
        + ".properties").format()).header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON).header(HttpHeaders
        .CONTENT_TYPE, MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, String.format("token " +
        "%s", token)).through(AutoRedirectingWire.class);
    return new RtGithub(REQUEST);
  }

  public Repo getRepo(String gitRepo, String token, String owner, String project) {
    return getGit(gitRepo, token).repos().get(new Coordinates.Simple(owner, project));
  }

  public List<String> getBranches(Repo repo) {
    List<String> branches = new LinkedList<String>();
    for (Branch branch : repo.branches().iterate()) {
      branches.add(branch.name());
    }
    logger.info("Git Branch List: {}", branches);
    return branches;
  }

  public List<String> getTagsByAPI(Repo repo, String gitApi, String user, String token) {
    String auth = user + ":" + token;
    byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
    String authHeader = "Basic " + new String(encodedAuth);
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.set("Authorization", authHeader);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response = restTemplate.exchange(gitApi + "/repos/" + repo.coordinates().user() + "/" +
        repo.coordinates().repo() + "/git/refs/tags", HttpMethod.GET, entity, String.class);
    if (response.getStatusCode() == HttpStatus.OK) {
      return JsonPath.read(response.getBody(), "$[*]['ref']");
    } else {
      return Lists.newArrayList();
    }
  }

  public List<String> getTags(Repo repo) {
    List<String> tags = Lists.newLinkedList();
    for (Reference tag : repo.git().references().tags()) {
      tags.add(tag.ref());
    }
    logger.info("Git Tag List: {}", tags);
    return tags;
  }

  public List<String> getRefs(Repo repo) {
    List<String> refs = Lists.newLinkedList();
    for (Reference ref : repo.git().references().iterate()) {
      refs.add(ref.ref());
    }
    logger.info("Git Ref List: {}", refs);
    return refs;
  }

  public Map<String, String> getFileShaAndRaw(Repo repo, String path, String branch) throws IOException {
    Map<String, String> file = new HashMap<>();
    Content content = repo.contents().get(path, branch);
    InputStream inputStream = content.raw();
    file.put("sha", new Content.Smart(content).sha());
    file.put("raw", IOUtils.toString(inputStream, "utf-8"));
    IOUtils.closeQuietly(inputStream);
    return file;
  }

  public boolean checkFileExist(Repo repo, String path, String branch) throws IOException {
    return repo.contents().exists(path, branch);
  }

  public String getFileSha(Repo repo, String path, String branch) throws IOException {
    return new Content.Smart(repo.contents().get(path, branch)).sha();
  }

  public List<Map<String, String>> getRepoFileList(Repo repo, String treeSha) throws IOException {
    JsonArray trees = repo.git().trees().get(treeSha).json().getJsonArray("tree");
    return trees.stream().map(tree -> convertTreeToMap(tree)).collect(Collectors.toList());

//        return files.entrySet().stream().sorted(Map.Entry.<String, String>
//                comparingByValue().reversed())
//                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
//                        (oldValue, newValue) -> oldValue,
//                        LinkedHashMap::new));
  }

  private Map<String, String> convertTreeToMap(JsonValue tree) {
    JsonObject treeObj = (JsonObject) tree;
    Map<String, String> treeMap = new HashMap<>();
    logger.info("Tree Object: {}", treeObj);
    treeMap.put("path", treeObj.getString("path"));
    treeMap.put("type", Optional.ofNullable(FileMode.valueOfMode(treeObj.getString("mode"))).map(file -> file
        .name()).orElse("Unknown"));
    treeMap.put("sha", treeObj.getString("sha"));
    return treeMap;
  }

  public String getRefSha(Repo repo, String branch) throws IOException {
    Reference reference = repo.git().references().get("refs/heads/" + branch.trim());
    return reference.json().getJsonObject("object").getString("sha");
  }

  public Map<String, String> getCommitAndTreeSha(Repo repo, String refSha) throws IOException {
    Commit commit = repo.git().commits().get(refSha);
    Map<String, String> commitContent = new HashMap<>();
    commitContent.put("commitSha", commit.sha());
    commitContent.put("treeSha", commit.json().getJsonObject("tree").getString("sha"));
    return commitContent;
  }

  public String createCommit(Repo repo, String treeSha, String parentSha, String message) throws IOException {
    JsonObject params = Json.createObjectBuilder().add("message", message).add("tree", treeSha).add("parents",
        Json.createArrayBuilder().add(parentSha)).build();
    return repo.git().commits().create(params).sha();
  }

  public String createBlob(Repo repo, String content) throws IOException {
    return repo.git().blobs().create(content, "utf-8").sha();
  }

  public String createTree(Repo repo, List<Map<String, String>> files, String baseTreeSha) throws IOException {
    JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
    files.stream().forEach(file -> jsonArrayBuilder.add(Json.createObjectBuilder().add("path", file.get("path"))
        .add("mode", "100644").add("type", "blob").add("sha", file.get("blobSha"))));
    JsonObject params = Json.createObjectBuilder().add("base_tree", baseTreeSha).add("tree", jsonArrayBuilder)
        .build();
    return repo.git().trees().create(params).sha();
  }

  public Reference createBranch(Repo repo, String parentBranch, String newBranch) throws IOException {
    Reference ref = repo.git().references().get("refs/heads/" + parentBranch.trim());
    logger.info("Create Branch {} base on Branch {}", newBranch, parentBranch);
    return createRef(repo, ref.json().getJsonObject("object").getString("sha"), "heads", newBranch);
  }

  public Reference createRef(Repo repo, String commitSha, String type, String ref) throws IOException {
    return repo.git().references().create("refs/" + type + "/" + ref.trim(), commitSha);
  }

  public void updateRef(Repo repo, String commitSha, String type, String ref) throws IOException {
    JsonObject json = Json.createObjectBuilder().add("sha", commitSha).add("force", true).build();
    repo.git().references().get("refs/" + type + "/" + ref.trim()).patch(json);
  }

  public Tag createTag(Repo repo, String objectSha, String type, String tag, String message) throws IOException {
    JsonObject tagparams = Json.createObjectBuilder().add("tag", tag).add("message", message).add("object",
        objectSha).add("type", type).build();
    return repo.git().tags().create(tagparams);
  }

  public Map<String, String> searchFileByPath(Repo repo, String keyword) throws IOException {
    String repoInfo = repo.coordinates().user() + "/" + repo.coordinates().repo();
    String qParam = String.format("filename:%s repo:%s", keyword, repoInfo);
    logger.info("Git search keyword: {}", qParam);
    JsonResponse resp = repo.github().entry().uri().path("/search/code").queryParam("q", qParam).back().fetch()
        .as(JsonResponse.class);
    logger.info("Git Search response: {}", resp);
    List<JsonObject> items = resp.json().readObject().getJsonArray("items").getValuesAs(JsonObject.class);
    Map<String, String> files = new HashMap<>();
    for (JsonObject item : items) {
      files.put(item.getString("path"), item.getString("sha"));
    }
    return files;
  }

  public enum FileMode {
    FILE_NORMAL("100644"), FOLDER("040000"), FILE_EXEC("100755"), FILE_LINK("120000");
    private String value;
    private static Map<String, FileMode> map = new HashMap<>();

    FileMode(String value) {
      this.value = value;
    }

    static {
      for (FileMode fileMode : FileMode.values()) {
        map.put(fileMode.value, fileMode);
      }
    }

    public static FileMode valueOfMode(String fileMode) {
      return map.get(fileMode);
    }

    public String getValue() {
      return value;
    }

  }

}
