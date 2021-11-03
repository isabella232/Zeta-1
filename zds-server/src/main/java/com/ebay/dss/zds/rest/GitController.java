package com.ebay.dss.zds.rest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.rest.error.ErrorDTO;
import com.ebay.dss.zds.rest.error.StackTraceError;
import com.ebay.dss.zds.rest.error.StringMessageError;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ebay.dss.zds.model.GithubRequest;
import com.ebay.dss.zds.service.GitService;

/**
 * Created by zhouhuang on 2018年12月13日
 */
@RestController
@RequestMapping("Git")
public class GitController {

    private static final Logger logger = LoggerFactory.getLogger(GitController.class);
    private static Map<String, String> GitException = ImmutableMap.of("401 Unauthorized", "Git Login Failed. Please Reset Github token", "Git Repository is empty", "Git Repository is empty. Please add README", "Reference already exists", "Tag have been existed");
    @Autowired
    private GitService gitService;

    @ExceptionHandler(value = {ToolSetCheckException.class, Exception.class})
    public ResponseEntity<ErrorDTO> handleException(Exception ex) {
        logger.error("Catch Exception:", ex);
        Optional<String> key = GitException.keySet().stream().filter(k -> ex.getMessage().contains(k)).findFirst();
        String exception = key.isPresent() ? GitException.get(key.get()) : ex.getMessage();

        if (ex instanceof ToolSetCheckException || key.isPresent()) {
            ErrorDTO dto = new ErrorDTO(ErrorCode.GIT_EXCEPTION, StringMessageError.from(exception));
            return ResponseEntity.status(((ErrorCode.WithHttpStatus) ErrorCode.GIT_EXCEPTION).status()).body(dto);
        } else {
            ErrorDTO dto = new ErrorDTO(ErrorCode.UNKNOWN_EXCEPTION, StackTraceError.fromThrowable(ex));
            return ResponseEntity.status(((ErrorCode.WithHttpStatus) ErrorCode.UNKNOWN_EXCEPTION).status()).body(dto);
        }
    }


    @RequestMapping(path = "/getBranches", method = RequestMethod.POST)
    public List<String> getBranchs(@RequestBody GithubRequest githubRequest) {
        return gitService.getBranchList(githubRequest);
    }

    @RequestMapping(path = "/createBranch", method = RequestMethod.POST)
    public List<String> createBranch(@RequestBody GithubRequest githubRequest) {
        return gitService.createBranch(githubRequest);
    }

    @RequestMapping(path = "/getGitFiles", method = RequestMethod.POST)
    public List<Map<String, String>> getGitFiles(@RequestBody GithubRequest githubRequest) {
        return gitService.getFileList(githubRequest);
    }

    @RequestMapping(path = "/searchFiles", method = RequestMethod.POST)
    public Map<String, String> searchFiles(@RequestBody GithubRequest githubRequest) {
        return gitService.searchFile(githubRequest);
    }

    @RequestMapping(path = "/checkNotebookExists", method = RequestMethod.POST)
    public Map<String, String> checkNotebookExists(@RequestBody GithubRequest githubRequest) {
        return gitService.checkNotebookExists(githubRequest);
    }

    @RequestMapping(path = "/pull", method = RequestMethod.POST)
    public void pull(@RequestBody GithubRequest githubRequest) {
        gitService.pullGitFiles(githubRequest);
    }

    @RequestMapping(path = "/checkGitFileExists", method = RequestMethod.POST)
    public Map<String, String> checkGitFileExists(@RequestBody GithubRequest githubRequest) {
        return gitService.checkGitFileExists(githubRequest);
    }

    @RequestMapping(path = "/push", method = RequestMethod.POST)
    public void push(@RequestBody GithubRequest githubRequest) {
        gitService.pushGitFiles(githubRequest);
    }

    @RequestMapping(path = "/checkFileExist", method = RequestMethod.POST)
    public boolean checkFileExist(@RequestBody GithubRequest githubRequest) {
        return gitService.checkFileExists(githubRequest);
    }

}
