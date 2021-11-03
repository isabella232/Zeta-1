package com.ebay.dss.zds.model.ace;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "zeta_alation_article")
public class AlationArticle {

    @Id
    public Integer id;
    public String url;
    public Integer questionId;

    public Integer getId() {
        return id;
    }

    public AlationArticle setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public AlationArticle setQuestionId(Integer questionId) {
        this.questionId = questionId;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public AlationArticle setUrl(String url) {
        this.url = url;
        return this;
    }
}
