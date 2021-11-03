package com.ebay.dss.zds.model.ace;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "zeta_alation_query")
public class AlationQuery {

    @Id
    private Integer id;
    private String url;
    private String content;

    public Integer getId() {
        return id;
    }

    public AlationQuery setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public AlationQuery setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getContent() {
        return content;
    }

    public AlationQuery setContent(String content) {
        this.content = content;
        return this;
    }
}
