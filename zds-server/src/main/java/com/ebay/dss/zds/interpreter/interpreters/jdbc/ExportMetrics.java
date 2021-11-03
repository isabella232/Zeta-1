package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import org.joda.time.DateTime;

/**
 * Created by tatian on 2020-07-20.
 */
public class ExportMetrics {

  public long fileSize;
  public long segmentSize;
  public long clientBufferSize;
  public long totalSourceFetchTime;
  public long totalClientTransferTime;
  public long totalSourceFetchCount;
  public long totalClientTransferCount;
  public long totalTransferTime;
  public OperationCost maxSourceFetchTime = new OperationCost();
  public OperationCost maxClientTransferTime = new OperationCost();

  public static class OperationCost {
    public long cost = 0;
    public long time = System.currentTimeMillis();
  }

  public void updateSourceFetchTime(long time) {
    totalSourceFetchTime += time;
    totalSourceFetchCount += 1;
    if (time > maxSourceFetchTime.cost) {
      maxSourceFetchTime.cost = time;
      maxSourceFetchTime.time = System.currentTimeMillis();
    }
  }

  public void updateClientTransferTime(long time) {
    totalClientTransferTime += time;
    totalClientTransferCount += 1;
    if (time > maxClientTransferTime.cost) {
      maxClientTransferTime.cost = time;
      maxClientTransferTime.time = System.currentTimeMillis();
    }
  }

  public double avgSourceFetchTime() {
    return totalSourceFetchTime > 0 ? totalSourceFetchTime * 1.0 / totalSourceFetchCount : 0;
  }

  public double avgClientTransferTime() {
    return totalClientTransferCount > 0 ? totalClientTransferTime * 1.0 / totalClientTransferCount : 0;
  }

  public double avgTransferBytePerSec() {
    return totalTransferTime > 0 ? fileSize * 1.0 / (totalTransferTime / 1000.0) : 0;
  }

  public String report() {
    return String.format("fileSize: %s,\n" +
                    "segmentSize: %s,\n" +
                    "clientBufferSize: %s,\n" +
                    "maxSourceFetchTime: %s,\n" +
                    "maxSourceFetchTimestamp: %s,\n" +
                    "maxClientTransferTime: %s,\n" +
                    "maxClientTransferTimestamp: %s,\n" +
                    "avgSourceFetchTime: %s,\n" +
                    "avgClientTransferTime: %s,\n" +
                    "totalSourceFetchCount: %s,\n" +
                    "totalClientTransferCount: %s,\n" +
                    "avgTransferBytePerSec: %s\n",
            fileSize,
            segmentSize,
            clientBufferSize,
            maxSourceFetchTime.cost, new DateTime(maxSourceFetchTime.time).toString("yyyy-MM-dd HH:mm:ss"),
            maxClientTransferTime.cost, new DateTime(maxClientTransferTime.time).toString("yyyy-MM-dd HH:mm:ss"),
            avgSourceFetchTime(),
            avgClientTransferTime(),
            totalSourceFetchCount,
            totalClientTransferCount,
            avgTransferBytePerSec()
    );
  }
}
