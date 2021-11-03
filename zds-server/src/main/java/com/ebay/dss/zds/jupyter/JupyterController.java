package com.ebay.dss.zds.jupyter;


import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


/**
 * Created by zhouhuang
 */
@RestController
@RequestMapping("/Jupyter")
public class JupyterController {


  @Autowired
  private JupyterConfService jupyterConfService;

  @PutMapping("/updatePySparkConf/{confName}")
  public String updatePySparkConf(
      @AuthenticationNT String nt
      , @PathVariable("confName") String confName
      , @RequestBody Map<String, String> confBody) {
    return jupyterConfService.updatePySparkConf(nt, confName, confBody);
  }

  @GetMapping("/getOrCreatePySparkConf/{confName}")
  public String getOrCreatePySparkConf(
      @AuthenticationNT String nt
      , @PathVariable("confName") String confName) {
    return jupyterConfService.getOrCreatePySparkConf(nt, confName);
  }

  @GetMapping("/getPySparkConfByUser/{confName}")
  public String getPySparkConfByUser(
      @PathVariable("confName") String confName
      , String nt) {
    return jupyterConfService.getOrCreatePySparkConf(nt, confName);
  }
}
