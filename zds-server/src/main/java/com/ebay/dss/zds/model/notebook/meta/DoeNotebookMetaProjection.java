package com.ebay.dss.zds.model.notebook.meta;

import org.springframework.beans.factory.annotation.Value;

public interface DoeNotebookMetaProjection {

    public String getId();

    @Value("#{target.description}")
    public String getDesc();

    @Value("#{target.notebook_type}")
    public String getType();

    public String getTitle();

    public String getReference();

}
