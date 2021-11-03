package com.ebay.dss.zds.service.filehandler;

import java.io.File;
import java.util.Map;

public class FileHandler {

  private FileHandlerStrategy fileHandlerStrategy;

  public FileHandler(FileHandlerStrategy fileHandlerStrategy) {
    this.fileHandlerStrategy = fileHandlerStrategy;
  }

  public Map<String, String> executeParseHeader(String fullPath) {
    return fileHandlerStrategy.parseHeader(fullPath);
  }

  public File executeWriteToFile(String fullPath, Object data, Map<String, String> schema) {
    return fileHandlerStrategy.writeToFile(fullPath, data, schema);
  }
}
