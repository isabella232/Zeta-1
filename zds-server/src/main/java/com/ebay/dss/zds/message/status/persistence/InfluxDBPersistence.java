package com.ebay.dss.zds.message.status.persistence;

import com.ebay.dss.zds.message.status.LivyStatementStatusStore;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by tatian on 2019-07-24.
 */
public class InfluxDBPersistence extends Persistence<LivyStatementStatusStore.StatusEntry> {

    private static final Logger logger = LoggerFactory.getLogger(InfluxDBPersistence.class);

    private String username;
    private String password;
    private String url;
    private String database;
    private String measurement;
    private String retentionPolicy;
    private ThreadLocal<InfluxDB> influxDB;

    public InfluxDBPersistence(Properties prop) {
        this.username = prop.getProperty("zds.track.persistence.influxDB.username");
        this.password = prop.getProperty("zds.track.persistence.influxDB.password");
        this.url = prop.getProperty("zds.track.persistence.influxDB.url");
        this.database = prop.getProperty("zds.track.persistence.influxDB.database");
        this.measurement = prop.getProperty("zds.track.persistence.influxDB.measurement");
        this.retentionPolicy = prop.getProperty("zds.track.persistence.influxDB.retentionPolicy", "2_hours");
        this.influxDB = ThreadLocal.withInitial(() -> {
            InfluxDB instance = InfluxDBFactory.connect(url, username, password);
            instance.setDatabase(database);
            instance.setRetentionPolicy(retentionPolicy);
            return instance;
        });
        influxDB.get().ping();
    }

    public InfluxDBPersistence(Environment prop) {
        this.username = prop.getProperty("zds.track.persistence.influxDB.username");
        this.password = prop.getProperty("zds.track.persistence.influxDB.password");
        this.url = prop.getProperty("zds.track.persistence.influxDB.url");
        this.database = prop.getProperty("zds.track.persistence.influxDB.database");
        this.measurement = prop.getProperty("zds.track.persistence.influxDB.measurement");
        this.retentionPolicy = prop.getProperty("zds.track.persistence.influxDB.retentionPolicy", "2_hours");
        this.influxDB = ThreadLocal.withInitial(() -> {
            InfluxDB instance = InfluxDBFactory.connect(url, username, password);
            instance.setDatabase(database);
            instance.setRetentionPolicy(retentionPolicy);
            return instance;
        });
        influxDB.get().ping();
    }

    public void persist(LivyStatementStatusStore.StatusEntry entry) {
        Map<String, String> tags = new HashMap<>();
        tags.put("statement_id", entry.statementId + "");
        tags.put("nt", entry.nt);
        tags.put("note_id", entry.noteId);
        tags.put("interpreter_class", entry.interpreterClass);

        Map<String, Object> fields = new HashMap<>();
        fields.put("status", entry.status.name());
        entry.metrics.keySet()
                .forEach( key -> {
                    String value = entry.metrics.get(key);
                    try {
                        long value_long = Long.valueOf(value);
                        fields.put(key, value_long);
                    } catch (NumberFormatException ex) {
                        fields.put(key, value);
                    }
                });
        Builder builder = Point.measurement(measurement);
        builder.tag(tags);
        builder.fields(fields);

        builder.time(entry.startTime.getMillis(), TimeUnit.MILLISECONDS);
        influxDB.get().write(database, retentionPolicy, builder.build());
    }
}
