package com.ebay.dss.zds.service.filehandler;

import com.ebay.dss.zds.common.FileHelper;
import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.google.common.collect.Maps;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CSVFileHandler implements FileHandlerStrategy {
  private final static Logger LOGGER = LoggerFactory.getLogger(CSVFileHandler.class);

  @Override
  public Map<String, String> parseHeader(String fullPath) {
    Reader reader = null;
    CSVReader csvReader = null;
    Map<String, String> header = Maps.newLinkedHashMap();

    try {
//      reader = Files.newBufferedReader(Paths.get(path), FileHelper.getFileEncoding(new File(path)));
      reader = new FileReader(fullPath);
      csvReader = new CSVReader(reader);
      String[] record = csvReader.readNext();
      if (Objects.isNull(record)) {
        throw new ToolSetCheckException("Get CSV file header failed. It seems the file is empty or damaged");
      }
      for (String str : record) {
        if (str.startsWith("\uFEFF")) {
          str = str.replace("\uFEFF", "");
        } else if (str.endsWith("\uFEFF")) {
          str = str.replace("\uFEFF", "");
        }
        header.put(str, "string");
      }
    } catch (IOException | CsvValidationException e) {
      LOGGER.error("Parse CSV File Failed!", e);
      throw new ToolSetCheckException("Parse CSV File Failed");
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          LOGGER.error("Close File Reader Failed!", e);
        }
      }
      if (csvReader != null) {
        try {
          csvReader.close();
        } catch (IOException e) {
          LOGGER.error("Close CSV Reader Failed!", e);
        }
      }
    }

    return header;
  }

  @Override
  public File writeToFile(String fullPath, Object data, Map<String, String> schemaInfo) {
    if (data instanceof List && ((List) data).size() > 0) {
      LOGGER.info("Write to CSV File: {}", fullPath);
      File file = new File(fullPath);
      FileHelper.mkdirIfNotExist(file.getParentFile());
      List<Map<String, Object>> dataList = (List<Map<String, Object>>) data;
      String[] header = dataList.get(0).keySet().stream().toArray(String[]::new);
      LOGGER.info("CSV File {} header: {}", file.getName(), header);
      boolean emptyFlag = true;

      try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
        writer.writeNext(header, false);
        for (Map<String, Object> d : dataList) {
          if (d.values().stream().filter(Objects::nonNull).count() == 0) {
            LOGGER.info("Skip Empty row");
            continue;
          }
          emptyFlag = false;
          writer.writeNext(d.values().stream().map(v -> Objects.nonNull(v)
              ? v.toString() : null).toArray(String[]::new));
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      return emptyFlag ? null : file;

    }
    return null;
  }
}
