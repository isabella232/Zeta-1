package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.ebay.dss.zds.interpreter.interpreters.livy.k8s.KubernetesUrls;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.LIVY_CONF_URL;

/**
 * Created by tatian on 2020-02-18.
 */
public class LivyURLSelectorFactory {

  public static final String FAKE_NT = "FAKE_NT";

  public static class LivyURLNTHashSelector implements LivyURLSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(LivyURLNTHashSelector.class);

    private String nt;
    private List<String> urls;
    private volatile String currentUrl;
    private Properties prop;

    public LivyURLNTHashSelector(Properties prop) {
      this.prop = prop;
      String livyUrls = prop.getProperty(LIVY_CONF_URL);
      if (StringUtils.isEmpty(livyUrls))
        throw new IllegalArgumentException("THe " + LIVY_CONF_URL + " is not configured");
      this.urls = flatMapUrls(livyUrls);
      selectCurrentUrl();
    }

    private List<String> flatMapUrls(String livyUrls) {
      String[] mixUrls = livyUrls.split(",");
      List<String> urls = new ArrayList<>();
      for (String tmpUrl : mixUrls) {
        if (tmpUrl.startsWith("http")) {
          urls.add(tmpUrl);
        } else if (tmpUrl.startsWith(KubernetesUrls.PREFIX)){
          LOGGER.info("This is an k8s urls: {}, try get from k8s api", tmpUrl);
          KubernetesUrls k8sUrl = KubernetesUrls.fromString(tmpUrl);
          List<String> endpoints = k8sUrl.getPodEndpoints();
          if (endpoints != null && endpoints.size() > 0) {
            LOGGER.info("Got endpoints: {} from: {}", String.join(",", endpoints), tmpUrl);
            urls.addAll(endpoints);
          } else {
            LOGGER.info("Got emtpy endpoints from: {}", tmpUrl);
          }
        }
      }
      return urls;
    }

    private void selectCurrentUrl() {
      int length = urls.size();
      if (length == 0) {
        throw new IllegalArgumentException("No any urls provided!");
      }
      String ntOptional = prop.getProperty("zeta.userId");
      int index;
      if (StringUtils.isEmpty(ntOptional)) {
        this.nt = FAKE_NT;
        index = new Random().nextInt(this.urls.size());
        LOGGER.warn("No zeta.userId configured, using random index: " + index);
      } else {
        this.nt = ntOptional;
        int hash = this.nt.hashCode();
        if (hash < 0) hash = hash * -1;
        index = hash % length;
      }
      currentUrl = urls.get(index);
      LOGGER.info("Url selected: {} for user: {}, index: {}", currentUrl, this.nt, index);
    }

    public String select() {
      return this.currentUrl;
    }

    public boolean hasNext() {
      return this.urls != null && this.urls.size() > 0;
    }

    public List<String> urls() {
      return this.urls;
    }

    public String removeUrl(String url) {
      this.urls.remove(url);
      LOGGER.info("Removed Livy url: {} for user: {}'s option", url, this.nt);
      selectCurrentUrl();
      return url;
    }
  }

  public static LivyURLSelector create(Properties prop) {
    return new LivyURLNTHashSelector(prop);
  }
}
