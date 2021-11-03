package com.ebay.dss.zds.model;

import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeReq;

import java.util.Map;

/**
 * Created by zhouhuang on 2018年11月13日
 */
public class NoteBookSchedulerJob {

    private int clusterId;

    private String host;

    private String jdbcType;

    private String proxyUser;

    private Map<String,String> prop;

    private ExecuteCodeReq req;

    /**
     * @return the clusterId
     */
    public int getClusterId() {
        return clusterId;
    }

    /**
     * @param clusterId the clusterId to set
     */
    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * @return the proxyUser
     */
    public String getProxyUser() {
        return proxyUser;
    }

    /**
     * @param proxyUser the proxyUser to set
     */
    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    /**
     * @return the req
     */
    public ExecuteCodeReq getReq() {
        return req;
    }

    /**
     * @param req the req to set
     */
    public void setReq(ExecuteCodeReq req) {
        this.req = req;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(String jdbcType) {
        this.jdbcType = jdbcType;
    }

    public Map<String, String> getProp() {
        return prop;
    }

    public void setProp(Map<String, String> prop) {
        this.prop = prop;
    }
}
