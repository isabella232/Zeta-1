package com.ebay.dss.zds.service.filehandler;

import java.io.File;
import java.util.Map;

public interface FileHandlerStrategy {

  Map<String, String> parseHeader(String path);

  File writeToFile(String path, Object data, Map<String, String> schemaInfo);

}
