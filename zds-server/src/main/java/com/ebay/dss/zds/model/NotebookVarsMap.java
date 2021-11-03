package com.ebay.dss.zds.model;

import com.ebay.dss.zds.common.JsonUtil;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tatian on 2020-10-27.
 */
public class NotebookVarsMap extends HashMap<String, NotebookVariable> {

  public static final String PROP_KEY = "var";

  public static NotebookVarsMap fromJson(String json) {
    return JsonUtil.GSON.fromJson(json, NotebookVarsMap.class);
  }

  public static NotebookVarsMap fromNotebook(ZetaNotebook notebook) {
    String preference = notebook.getPreference();
    if (StringUtils.isNotEmpty(preference)) {
      return fromNotebookPreference(preference);
    } else {
      return null;
    }
  }

  public static NotebookVarsMap fromNotebookPreference(String preference) {
   return fromNotebookPreference(ZetaNotebookPreference.fromJson(preference));
  }

  public static NotebookVarsMap fromNotebookPreference(ZetaNotebookPreference preference) {

    if (preference.variables != null) {
      Map<String, String> variables = preference.variables;
      Map<String, String> vargenerators = preference.vargenerators;
      NotebookVarsMap map = new NotebookVarsMap();
      for (String varName : variables.keySet()) {
        String value = variables.get(varName);
        String vargenerator = null;
        if (vargenerators != null) {
          vargenerator = vargenerators.get(varName);
        }
        map.put(varName, new NotebookVariable(varName, value, vargenerator));
      }
      return map;
    } else {
      return null;
    }
  }

  public String toJson() {
    return JsonUtil.GSON.toJson(this);
  }
}
