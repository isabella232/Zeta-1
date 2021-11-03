package com.ebay.dss.zds.service;

import com.ebay.dss.zds.common.DateUtil;
import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.dao.ReleaseHistoryRepository;
import com.ebay.dss.zds.model.ReleaseHistory;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by zhouhuang on 2019/3/8.
 */
@Service
public class ReleaseService {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseService.class);

    @Resource(name = "resttemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ReleaseHistoryRepository releaseHistoryRepository;

    @Value("${release.host.url}")
    private String releaseHost;

    public Object checkJira(String nt, String tag, String jiraName) {
        String url = String.format(releaseHost + "jira/checkStatus?nt=%s&releaseTag=%s&issueName=%s", nt, tag,
                jiraName);
        return restTemplate.getForObject(url, Object.class);
    }

    public Object getStatus(String nt, String releaseTag) {
        String url = String.format(releaseHost + "release/status/%s", releaseTag);
        logger.info("Get Status URL {}", url);
        String resp = restTemplate.getForObject(url, String.class);
        String isDone = JsonPath.read(resp, "$.status.value");
        logger.info("Check Job status {}", isDone);
        if ("done".equalsIgnoreCase(isDone)) {
            Map<String, String> execTask = JsonPath.read(resp, "$.execTask");
            if (execTask.values().stream().filter(value -> (!"0".equals(value))).count() > 0) {
                ReleaseHistory releaseHistory = new ReleaseHistory(nt, releaseTag, JsonUtil.toJson(execTask), new
                        Date());
                if (releaseHistoryRepository.countByReleaseTag(releaseTag) == 0) {
                    releaseHistoryRepository.save(releaseHistory);
                }
            }
        }
        return resp;
    }

    public List<ReleaseHistory> getHistory(String nt) {
        return releaseHistoryRepository.findCurrentHistory(nt, DateUtil.currentDate());
    }

    public Object manifest(Object req) {
        return postService(releaseHost + "api/release/manifest", req);
    }

    public Object sendValidateMail(Object req) {
        return postService(releaseHost + "api/release/sendValidateMail", req);
    }

    public Object rollout(Object req) {
        return postService(releaseHost + "api/release/rollout", req);
    }

    private Object postService(String url, Object object) {
        try {
            HttpEntity<Object> request = new HttpEntity<>(object);
            return restTemplate.exchange(url, HttpMethod.POST, request, String.class).getBody();
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getRawStatusCode()).headers(e.getResponseHeaders()).body(e
                    .getResponseBodyAsString());
        }
    }
}
