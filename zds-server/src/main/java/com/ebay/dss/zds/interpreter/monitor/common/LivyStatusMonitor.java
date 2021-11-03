package com.ebay.dss.zds.interpreter.monitor.common;

import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient;
import com.ebay.dss.zds.message.endpoint.PrometheusMetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2020-02-18.
 */
// todo: implement maybe
// @Component
public class LivyStatusMonitor {

  private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricRegistry.class);

  @Autowired
  ConfigurationManager configurationManager;

  private Map<Integer, LivyClient> livyClients;

  private ConcurrentHashMap<Integer, List<String>> aliveLivy;

  @PostConstruct
  public void init() {
    livyClients = new HashMap<>();
    InterpreterConfiguration conf = configurationManager.getDefaultConfiguration();

    // areslvs
    livyClients.put(2, new LivyClient(conf.mapClusterIdToUrl(2).getProperties()));

    // herculeslvs
    livyClients.put(10, new LivyClient(conf.mapClusterIdToUrl(10).getProperties()));

    // herculessublvs
    livyClients.put(11, new LivyClient(conf.mapClusterIdToUrl(11).getProperties()));

    // apollorno
    livyClients.put(14, new LivyClient(conf.mapClusterIdToUrl(14).getProperties()));

    aliveLivy = new ConcurrentHashMap<>();

    // areslvs
    CopyOnWriteArrayList aresUrls = new CopyOnWriteArrayList<String>();
    aresUrls.addAll(livyClients.get(2).getURLSelector().urls());
    aliveLivy.put(2, aresUrls);

    // herculeslvs
    CopyOnWriteArrayList herculesUrls = new CopyOnWriteArrayList<String>();
    herculesUrls.addAll(livyClients.get(10).getURLSelector().urls());
    aliveLivy.put(10, herculesUrls);

    // herculessublvs
    CopyOnWriteArrayList herculessubUrls = new CopyOnWriteArrayList<String>();
    herculessubUrls.addAll(livyClients.get(11).getURLSelector().urls());
    aliveLivy.put(11, herculessubUrls);

    // apollorno
    CopyOnWriteArrayList apollornoUrls = new CopyOnWriteArrayList<String>();
    apollornoUrls.addAll(livyClients.get(14).getURLSelector().urls());
    aliveLivy.put(14, apollornoUrls);
  }
}
