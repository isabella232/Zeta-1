package com.ebay.dss.zds.service;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.dao.GitHistoryRepository;
import com.ebay.dss.zds.model.GitHistory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ebay.dss.zds.common.GitUtil;
import com.ebay.dss.zds.common.LogUtil;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.model.GithubRequest;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.jcabi.github.Repo;

/**
 * Created by zhouhuang on 2018年12月13日
 */
@Service
public class GitService {

  private static final Logger logger = LoggerFactory.getLogger(GitService.class);
  private static final String SLASH = "/";

  @Autowired
  private ZetaUserService zetaUserService;

  @Autowired
  private ZetaNotebookRepository zetaNotebookRepository;

  @Autowired
  private GitUtil gitUtil;

  @Autowired
  private GitHistoryRepository gitHistoryRepository;

  private final String GIT_API_TEMPLATE = "https://%s/api/v3";

  protected Repo getRepo(GithubRequest githubRequest) {
    List<String> gitInfo = gitUtil.parseGitURL(githubRequest.getUrl());
    logger.info("Git URL Parse: {}", gitInfo);
    String token = Optional.ofNullable(zetaUserService.getGithubToken(githubRequest.getNt())).orElseThrow(() ->
        new ToolSetCheckException("Please setup Github token in User Settings Page!"));
    githubRequest.setToken(token);
    String gitApi = String.format(GIT_API_TEMPLATE, gitInfo.get(0));
    githubRequest.setGitApi(gitApi);
    logger.info("Get Git Token {}", LogUtil.displayableSecret(token, 2, 2, 50));
    logger.info("Get git Api {}", gitApi);
    return gitUtil.getRepo(gitApi, token, gitInfo.get(1), gitInfo.get(2));
  }

  public List<String> createBranch(GithubRequest githubRequest) {
    try {
      List<String> branchList = getBranchList(githubRequest);
      if (branchList.contains(githubRequest.getNewBranch())) {
        throw new ToolSetCheckException(String.format("Branch %s have been existed!", githubRequest
            .getNewBranch()));
      }
      gitUtil.createBranch(getRepo(githubRequest), githubRequest.getBranch(), githubRequest.getNewBranch());
      branchList.add(githubRequest.getNewBranch());
      return branchList;
    } catch (IOException e) {
      logger.error(e.toString());
      throw new ToolSetCheckException(e.getMessage());
    }
  }

  public List<String> getBranchList(GithubRequest githubRequest) {
    return gitUtil.getBranches(getRepo(githubRequest));
  }

  public List<Map<String, String>> getFileList(GithubRequest githubRequest) {
    try {
      Repo repo = getRepo(githubRequest);
      String treeSha = Optional.ofNullable(githubRequest.getFolderSha()).orElse(gitUtil.getCommitAndTreeSha
          (repo, gitUtil.getRefSha(repo, githubRequest.getBranch())).get("treeSha"));
      return gitUtil.getRepoFileList(repo, treeSha);
    } catch (IOException e) {
      logger.error("Exception: ", e);
      throw new ToolSetCheckException(e.getMessage());
    }
  }

  public Map<String, String> searchFile(GithubRequest githubRequest) {
    try {
      return gitUtil.searchFileByPath(getRepo(githubRequest), githubRequest.getSearchKeyword());
    } catch (IOException e) {
      logger.error("Exception: ", e);
      throw new ToolSetCheckException(e.getMessage());
    }
  }

  public Map<String, String> checkNotebookExists(GithubRequest githubRequest) {
    String wsPath = getWsPath(githubRequest.getWsPath());
    List<String> fullPaths = githubRequest.getFileList().stream().map(fileParam -> wsPath + fileParam.get("path")
    ).collect(Collectors.toList());
    logger.info("Check User {} has notebook {}", githubRequest.getNt(), fullPaths);
    List<String> notebooks = zetaNotebookRepository.getNotebooksByNtAndPaths(githubRequest.getNt(), fullPaths);
    return notebooks.stream().map(book -> book.substring(wsPath.length(), book.length())).distinct().collect
        (Collectors.toMap(Function.identity(), s -> "existed"));
  }

  private String getWsPath(String path) {
    return path.endsWith(SLASH) ? path : path + SLASH;
  }

  public void pullGitFiles(GithubRequest githubRequest) {
    Repo repo = getRepo(githubRequest);
    Set<String> updateFiles = checkNotebookExists(githubRequest).keySet();
    List<String> insertFiles = githubRequest.getFileList().stream().map(fileParam -> fileParam.get("path"))
        .collect(Collectors.toList());
    insertFiles.removeAll(updateFiles);
    logger.info("Insert Files: {}", insertFiles);
    logger.info("Update Files: {}", updateFiles);
    List<ZetaNotebook> insertBooks = insertFiles.stream().map(path -> generateNotebook(repo, githubRequest, path)
    ).collect(Collectors.toList());
    zetaNotebookRepository.batchAddNotebook(insertBooks);
    List<ZetaNotebook> updateBooks = updateFiles.stream().map(path -> generateNotebook(repo, githubRequest, path)
    ).collect(Collectors.toList());
    zetaNotebookRepository.batchUpdateNotebook(updateBooks);
  }

  private ZetaNotebook generateNotebook(Repo repo, GithubRequest githubRequest, String path) {
    try {
      Map<String, String> file = gitUtil.getFileShaAndRaw(repo, path, githubRequest.getBranch());
      ZetaNotebook zetaNotebook = new ZetaNotebook();
      zetaNotebook.setNt(githubRequest.getNt());
      zetaNotebook.setTitle(path.substring(path.lastIndexOf(SLASH) + 1, path.length()));
      zetaNotebook.setContent(file.get("raw"));
      zetaNotebook.setPath(getWsPath(githubRequest.getWsPath()) + path.substring(0, path.lastIndexOf(SLASH) + 1));
      zetaNotebook.setSha(file.get("sha"));
      zetaNotebook.setGitRepo(repo.coordinates().user() + SLASH + repo.coordinates().repo());
      return zetaNotebook;
    } catch (IOException e) {
      logger.error("Exception: ", e);
      throw new ToolSetCheckException("Read [" + path + "] content failed!");
    }
  }

  public Map<String, String> checkGitFileExists(GithubRequest githubRequest) {
    Map<String, String> existFiles = new HashMap<>();
    Repo repo = getRepo(githubRequest);
    try {
      for (Map<String, String> fileParam : githubRequest.getFileList()) {
        logger.info("GithubRequest FileParam {}", fileParam);
        String fullPath = fileParam.get("fullPath");
        logger.info("Check File {} exist in Github", fullPath);
        if (gitUtil.checkFileExist(repo, fullPath, Optional.ofNullable(githubRequest.getBranch()).orElse
            ("master"))) {
          if (fileParam.containsKey("sha") && StringUtils.isNotBlank(fileParam.get("sha"))) {
            String gitSha = gitUtil.getFileSha(repo, fullPath, Optional.ofNullable(githubRequest
                .getBranch()).orElse("master"));
            logger.info("Get File {} Latest Sha {}", fullPath, gitSha);
            if (!fileParam.get("sha").equals(gitSha)) {
              logger.info("Occur Github File Conflict, File {} before sha{}, now sha {}", fullPath,
                  fileParam.get("sha"), gitSha);
              existFiles.put(fullPath, "conflicted");
            }
          } else {
            existFiles.put(fullPath, "existed");
          }
        }
      }
      return existFiles;
    } catch (IOException e) {
      logger.error("Exception: ", e);
      throw new ToolSetCheckException(e.getMessage());
    }
  }

  public void pushGitFiles(GithubRequest githubRequest) {
    Repo repo = getRepo(githubRequest);
    if (StringUtils.isNotBlank(githubRequest.getTag()) &&
        gitUtil.getTagsByAPI(repo, githubRequest.getGitApi(),
            githubRequest.getNt(), githubRequest.getToken())
            .contains("refs/tags/" + githubRequest.getTag())) {
      throw new ToolSetCheckException("Tag have been existed");
    }
    try {
      String refSha = gitUtil.getRefSha(repo, Optional.ofNullable(githubRequest.getBranch()).orElse("master"));
      Map<String, String> commit = gitUtil.getCommitAndTreeSha(repo, refSha);
      String commitSha = commit.get("commitSha");
      String treeSha = commit.get("treeSha");
      List<Map<String, String>> files = Lists.newLinkedList();
      List<String> notebooksId = Lists.newLinkedList();
      for (Map<String, String> fileParam : githubRequest.getFileList()) {
        notebooksId.add(fileParam.get("notebookId"));
        String content = zetaNotebookRepository.getNotebook(fileParam.get("notebookId")).getContent();
        String blobSha = gitUtil.createBlob(repo, content);
        Map<String, String> map = Maps.newLinkedHashMap();
        String fullPath = fileParam.get("fullPath");
        map.put("path", fullPath.startsWith(SLASH) ? fullPath.substring(1) : fullPath);
        map.put("blobSha", blobSha);
        files.add(map);
      }
      logger.info("Push File List {}", files);
      String newTreeSha = gitUtil.createTree(repo, files, treeSha);
      String newCommitSha = gitUtil.createCommit(repo, newTreeSha, commitSha, githubRequest.getCommitMessage());
      if (StringUtils.isNotBlank(githubRequest.getTag())) {
        //                gitUtil.createTag(repo, newCommitSha, "commit", githubRequest.getTag(), githubRequest
        //                        .getCommitMessage());
        //                gitUtil.updateRef(repo, newCommitSha, "tags", githubRequest.getTag());
        gitUtil.createRef(repo, newCommitSha, "tags", githubRequest.getTag());
      }
      if (StringUtils.isNotBlank(githubRequest.getBranch())) {
        gitUtil.updateRef(repo, newCommitSha, "heads", githubRequest.getBranch());
      }
      zetaNotebookRepository.batchUpdateNotebookGitInfo(newCommitSha, repo.coordinates().user() + SLASH + repo
          .coordinates().repo(), notebooksId);
      GitHistory gitHistory = new GitHistory(githubRequest.getNt(), githubRequest.getUrl(), Optional.ofNullable
          (githubRequest.getBranch()).orElse("master"), githubRequest.getTag(), newCommitSha, JsonUtil
          .toJson(githubRequest.getFileList()), new Date());
      gitHistoryRepository.save(gitHistory);

    } catch (IOException e) {
      logger.error("Exception: ", e);
      throw new ToolSetCheckException(e.getMessage());
    }
  }

  public boolean checkFileExists(GithubRequest githubRequest) {
    try {
      return gitUtil.checkFileExist(getRepo(githubRequest), githubRequest.getFileList().get(0).get("fullPath"),
          githubRequest.getBranch());
    } catch (IOException e) {
      logger.error("Exception: ", e);
      throw new ToolSetCheckException(e.getMessage());
    }
  }

}
