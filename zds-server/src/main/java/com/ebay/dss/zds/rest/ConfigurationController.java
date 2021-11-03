package com.ebay.dss.zds.rest;


import com.ebay.dss.zds.common.PropertiesUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/conf")
public class ConfigurationController {


  @GetMapping("/scheduler/reload")
  public void reloadSchedulerConfiguration() {
    PropertiesUtil.loadSchedulerConfiguration();
  }
}
