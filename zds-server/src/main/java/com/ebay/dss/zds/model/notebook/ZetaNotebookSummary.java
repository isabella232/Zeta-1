package com.ebay.dss.zds.model.notebook;

import org.springframework.beans.factory.annotation.Value;

import java.sql.Timestamp;

public interface ZetaNotebookSummary {

    public String getId();

    @Value("#{target.is_public}")
    public String getIsPublic();

    @Value("#{target.description}")
    public String getDesc();

    public String getType();

    public String getPreference();

    public String getTitle();

    public String getNt();

    public Timestamp getLastUpdateDt();

    public Timestamp getLastRunDt();

    public int getFavoriteCnt();

    public int getExecutedCnt();
}
