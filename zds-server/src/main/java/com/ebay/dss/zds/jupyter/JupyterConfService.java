package com.ebay.dss.zds.jupyter;


import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.exception.ClusterAccessDeniedException;
import com.ebay.dss.zds.service.BDPHTTPService;
import io.jsonwebtoken.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import com.ebay.dss.zds.interpreter.ConfigurationManager.Cluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class JupyterConfService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JupyterConfService.class);

  @Autowired
  private JupyterConfRepository jupyterConfRepository;

  @Autowired
  private BDPHTTPService bdphttpService;

  private static final String PROXY_USER_KEY = "jupyter.proxyUser";
  private static final String USER_QUEUE_KEY = "spark.yarn.queue";
  private static final String NO_ACCESS_ALERT = "User: %s has no access to %s";


  public String updatePySparkConf(
      String nt, String confName, Map<String, String> confBody) {
    LOGGER.info("Update User-{} PySparkConf-{} to {}", nt, confName, confBody);
    String proxyUser = confBody.get(PROXY_USER_KEY);
    String userQueue = confBody.get(USER_QUEUE_KEY);
    Assert.hasText(proxyUser, "Proxy User is empty");
    Assert.hasText(userQueue, "User Queue is empty");
    checkUserAccessible(nt);
    Assert.isTrue(getApolloRnoBatchInfo(nt).contains(proxyUser)
        , String.format(NO_ACCESS_ALERT, nt, proxyUser));
    Assert.isTrue(getApolloRnoQueueInfo(nt).contains(userQueue)
        , String.format(NO_ACCESS_ALERT, nt, userQueue));
    return jupyterConfRepository.updatePysparkConf(nt, confName, JsonUtil.toJson(confBody));
  }

  public String getOrCreatePySparkConf(String nt, String confName) {
    LOGGER.info("Get User-{} PySparkConf-{}", nt, confName);
    try {
      return jupyterConfRepository.getUserPySparkConf(nt, confName);
    } catch (IncorrectResultSizeDataAccessException e) {
      checkUserAccessible(nt);
      return jupyterConfRepository.insertPysparkConf(nt, confName
          , getDefaultPySparkConf(nt, Cluster.apollorno.name()));
    }
  }

  private void checkUserAccessible(String nt) {
    if (Objects.isNull(getApolloRnoBatchInfo(nt))
        || Objects.isNull(getApolloRnoQueueInfo(nt))) {
      throw new ClusterAccessDeniedException(String.format(
          "User: %s has no access to cluster: %s. Please make sure you have the permission to access this cluster.\n" +
              "Check and request for cluster access: https://bdp.corp.ebay.com/cluster/request", nt, Cluster.apollorno.name()));
    }
  }

  private String getDefaultPySparkConf(String nt, String cluster) {
    List<String> queues = bdphttpService.getUserQueueInfo(cluster, nt);
    return PropertiesUtil.getJupyterProperties("jupyter.pyspark.conf")
        .replace("{nt}", nt)
        .replace("{queue}", queues.get(0));
  }

  private List<String> getApolloRnoBatchInfo(String nt) {
    return bdphttpService.getUserBatchInfo(Cluster.apollorno.name(), nt);
  }


  private List<String> getApolloRnoQueueInfo(String nt) {
    return bdphttpService.getUserQueueInfo(Cluster.apollorno.name(), nt);
  }

}
