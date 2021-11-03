package com.ebay.dss.zds.service.filehandler;


import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.service.metatable.ZetaMetaTableService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.avro.AvroSchemaConverter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.schema.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ParquetFileHandler implements FileHandlerStrategy {
  private final static Logger LOGGER = LoggerFactory.getLogger(ParquetFileHandler.class);

  @Override
  public Map<String, String> parseHeader(String fullPath) {
    ParquetReader<GenericData.Record> reader = null;
    Map<String, String> header = null;
    try {
      reader = AvroParquetReader
          .<GenericData.Record>builder(new Path(fullPath))
          .withConf(new Configuration())
          .build();
      GenericData.Record record;
      if ((record = reader.read()) != null) {
        List<Schema.Field> fieldList = record.getSchema().getFields();
        header = getHeader(fieldList.toArray(new Schema.Field[0]));
      } else {
        throw new ToolSetCheckException("Get Parquet file header failed. It seems the file is empty or damaged");
      }
    } catch (IOException e) {
      LOGGER.error("Parse Parquet File Failed!", e);
      throw new ToolSetCheckException("Parse Parquet File Failed");
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          LOGGER.error("Close Parquet Reader Failed!", e);
        }
      }
    }

    return header;
  }

  @Override
  public File writeToFile(String fullPath, Object data, Map<String, String> schemaInfo) {
    if (data instanceof List && ((List) data).size() > 0) {
      LOGGER.info("Write to Parquet File: {}", fullPath);
      List<Map<String, Object>> dataList = (List<Map<String, Object>>) data;
      /*
      Schema jsonSchema = JsonUtil.inferSchema(
      JsonUtil.parse(com.ebay.dss.zds.common.JsonUtil.toJson(dataList.get(0))), "metaschema");
       */
      Schema schema = generateSchema("sheetschema", schemaInfo);
      try (
          ParquetWriter<GenericData.Record> writer = AvroParquetWriter.
              <GenericData.Record>builder(new Path(fullPath))
              .withRowGroupSize(ParquetWriter.DEFAULT_BLOCK_SIZE)
              .withPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
              .withSchema(schema)
              .withConf(new Configuration())
              .withCompressionCodec(CompressionCodecName.SNAPPY)
              .withValidation(false)
              .withDictionaryEncoding(false)
              .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
              .build()
      ) {
        dataList.forEach((d) -> {
          try {
            GenericData.Record record = new GenericData.Record(schema);
            d.forEach((K, V) -> {
              Object obj = Objects.nonNull(V) ? V : schema.getField(K).defaultVal();
              record.put(K, obj);
            });
            writer.write(record);
          } catch (IOException e) {
            LOGGER.error("Write to file failed", e);
            throw new ToolSetCheckException("Write To Parquet File Failed");
          }
        });
        return new File(fullPath);
      } catch (IOException e) {
        LOGGER.error("Write to file failed", e);
        throw new ToolSetCheckException("Write To Parquet File Failed");
      }
    }
    return null;
  }

  private MessageType generateMessageType(String name, Map<String, String> schema) {
    if (Objects.nonNull(schema) && !schema.isEmpty()) {
      List<Schema.Field> recordFields = Lists.newArrayListWithExpectedSize(
          schema.size());

      for (Map.Entry<String, String> entry : schema.entrySet()) {
        Object defaultValue = null;
        Schema.Field field = new Schema.Field(
            entry.getKey(), getColumnSchema(entry.getValue()),
            entry.getValue(), defaultValue);
        recordFields.add(field);
      }
      Schema recordSchema =
          Schema.createRecord(name, null, null, false, recordFields);
      return convertType(recordSchema);
    }
    throw new ToolSetCheckException("Please Supply Schema Info");
  }

  private Schema generateSchema(String name, Map<String, String> schema) {
    if (Objects.nonNull(schema) && !schema.isEmpty()) {
      List<Schema.Field> recordFields = Lists.newArrayListWithExpectedSize(
          schema.size());

      for (Map.Entry<String, String> entry : schema.entrySet()) {
        Object defaultValue = null;
        Schema.Field field = new Schema.Field(
            entry.getKey(), getColumnSchema(entry.getValue()),
            entry.getValue(), defaultValue);
        recordFields.add(field);
      }
      Schema recordSchema =
          Schema.createRecord(name, null, null, false, recordFields);
      return recordSchema;
    }
    throw new ToolSetCheckException("Please Supply Schema Info");
  }

  private Schema getColumnSchema(String type) {
    switch (ZetaMetaTableService.ColumnType.valueOf(type.toUpperCase().trim())) {
      case INT:
        return Schema.create(Schema.Type.INT);
      case BIGINT:
        return Schema.create(Schema.Type.LONG);
      case DOUBLE:
        return Schema.create(Schema.Type.DOUBLE);
      case DECIMAL:
        return LogicalTypes.decimal(16, 4).addToSchema(Schema.create(Schema.Type.BYTES));
      case DATE:
        return LogicalTypes.date().addToSchema(Schema.create(Schema.Type.INT));
      case TIMESTAMP:
        return LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG));
      case VARCHAR:
      case STRING:
        return Schema.create(Schema.Type.STRING);
      default:
        throw new IllegalArgumentException("Unsupported column type: " + type);
    }
  }

  private MessageType convertType(Schema schema) {
    MessageType type = new AvroSchemaConverter(new Configuration()).convert(schema);
//    return new GroupType(Type.Repetition.OPTIONAL, schema.getFullName(), type.getFields());
    return type;
  }

  private Map<String, String> getHeader(Schema.Field[] fields) {
    Map<String, String> header = Maps.newLinkedHashMap();
    for (final Schema.Field field : fields) {
      header.put(field.name(), field.schema().getType().getName());
    }
    return header;
  }
}
