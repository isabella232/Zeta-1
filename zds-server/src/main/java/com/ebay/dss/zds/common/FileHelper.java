package com.ebay.dss.zds.common;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);

  public static void writeFile(String sb, String filePath, String fileName) throws IOException {
    File path = new File(filePath);
    mkdirIfNotExist(path);
    writeOutputStreamSilentOnException(sb, new FileWriter(filePath + File.separator + fileName));
  }

  public static void mkdirIfNotExist(File path) {
    if (!path.exists()) {
      path.mkdirs();
    }
  }

  public static void writeOutputStreamSilentOnException(String content, OutputStreamWriter writer) {
    try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
      bufferedWriter.write(content);
    } catch (IOException e) {
      LOGGER.error("Write File Fail!", e);
    }
  }

  public static String readFile(String filePath) throws FileNotFoundException {
    return readInputStreamReturnAlreadyReadOnException(new FileReader(filePath));
  }

  public static String readInputStreamReturnAlreadyReadOnException(InputStreamReader reader) {
    StringBuilder sb = new StringBuilder();
    try (BufferedReader bufferedReader = new BufferedReader(reader)) {
      String tempString;
      while ((tempString = bufferedReader.readLine()) != null) {
        sb.append(tempString).append("\n");
      }
    } catch (FileNotFoundException e) {
      LOGGER.error("File not Exist!", e);
    } catch (IOException e) {
      LOGGER.error("Read File Fail!", e);
    }
    return sb.toString();
  }

  public static void getFileList(String path, List<Map<String, String>> fileList) {
    File file = new File(path);
    fileList.addAll(getFileList(file));
  }

  public static List<Map<String, String>> getFileList(File root) {
    List<Map<String, String>> fileInfo = new ArrayList<>();
    if (root.exists()) {
      File[] files = root.listFiles();
      if (Objects.nonNull(files) && files.length > 0) {
        for (File file : files) {
          if (file.isDirectory()) {
            fileInfo.addAll(getFileList(file));
          } else {
            Map<String, String> resMap = new HashMap<>();
            resMap.put("filePath", file.getParent() + File.separator);
            resMap.put("fileName", file.getName());
            resMap.put("createTime", Long.toString(file.lastModified()));
            fileInfo.add(resMap);
          }
        }
      }
    }
    return fileInfo;
  }

  public static boolean deleteFile(String path) {
    File file = new File(path);
    if (!file.exists() || !file.isFile()) {
      return false;
    }
    return file.delete();
  }

  public static Charset getFileEncoding(File file) {
    Charset code = StandardCharsets.UTF_8;
    try {
      BufferedInputStream bin = new BufferedInputStream(new FileInputStream(file));
      int p = (bin.read() << 8) + bin.read();
      switch (p) {
        case 0xefbb:
          code = StandardCharsets.UTF_8;
          break;
        case 0xfeff:
          code = StandardCharsets.UTF_16BE;
          break;
        case 0x5c75:
          code = StandardCharsets.US_ASCII;
          break;
      }
      LOGGER.info(code.name());
      return code;
    } catch (IOException e) {
      LOGGER.error("File parse encoding failed!", e);
    }
    return code;
  }
}
