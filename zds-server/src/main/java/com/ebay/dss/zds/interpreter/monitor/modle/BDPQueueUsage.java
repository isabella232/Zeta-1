package com.ebay.dss.zds.interpreter.monitor.modle;

import com.ebay.dss.zds.common.JsonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tatian on 2020-06-23.
 */
public class BDPQueueUsage {

  public Map<String, Double> usage;

  public BDPQueueUsage(String json) {
    this.usage = toMap(json);
  }

  public Map<String, Double> toMap() {
    return this.usage;
  }

  public static Map<String, Double> toMap(String json) {
    HashMap<String, Double> map = new HashMap<>();
    try {
      JsonObject root = JsonUtil.jsonParser.parse(json).getAsJsonObject();
      JsonArray list = root.get("result").getAsJsonArray();
      if (list.size() > 0) {
        JsonObject subList = list.get(0).getAsJsonObject();
        for (JsonElement userUsage : subList.get("value").getAsJsonArray()) {
          JsonObject usage = userUsage.getAsJsonObject();
          String user = usage.get("key").getAsString();
          Double usageDouble = findUsedPercentage(usage.getAsJsonArray("value"));
          map.put(user, usageDouble);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return map;
  }

  private static Double findUsedPercentage(JsonArray ja) throws Exception {
    for (JsonElement e : ja) {
      JsonObject jo = e.getAsJsonObject();
      if ("clusterUsagePercentage".equals(jo.get("key").getAsString())) {
        return jo.get("value").getAsDouble();
      }
    }
    throw new Exception("Can't not find used percentage");
  }

  public static BDPQueueUsage fromJson(String json) {
    return new BDPQueueUsage(json);
  }

}
