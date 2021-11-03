package com.ebay.dss.zds.model;

public class KylinReadable {

    private String uuid;
    private long last_modified;
    private String version;
    private String name ;
    private String[] tables;
    private String owner;
    private String status;

    public String getUuid() {
        return uuid;
    }

    public KylinReadable setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public long getLast_modified() {
        return last_modified;
    }

    public KylinReadable setLast_modified(long last_modified) {
        this.last_modified = last_modified;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public KylinReadable setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getName() {
        return name;
    }

    public KylinReadable setName(String name) {
        this.name = name;
        return this;
    }

    public String[] getTables() {
        return tables;
    }

    public KylinReadable setTables(String[] tables) {
        this.tables = tables;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public KylinReadable setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public KylinReadable setStatus(String status) {
        this.status = status;
        return this;
    }
}
