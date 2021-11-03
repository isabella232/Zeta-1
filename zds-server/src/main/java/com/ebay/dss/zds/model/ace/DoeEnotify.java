package com.ebay.dss.zds.model.ace;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;

public class DoeEnotify {

    private static final String TIMEZONE = "America/Los_Angeles";

    private Long id;
    private AceEnotifyOptions.Type type;
    private String title;
    private URI link;
    private ZonedDateTime createTime;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private AceEnotifyOptions.Product product;

    public DoeEnotify() {
    }

    @JsonGetter("product")
    public AceEnotifyOptions.Product getProduct() {
        return product;
    }

    @JsonSetter("product_id")
    public DoeEnotify setProduct(AceEnotifyOptions.Product product) {
        this.product = product;
        return this;
    }

    @JsonGetter("id")
    public Long getId() {
        return id;
    }

    @JsonSetter("enotify_id")
    public DoeEnotify setId(Long id) {
        this.id = id;
        return this;
    }

    @JsonGetter("type")
    public AceEnotifyOptions.Type getType() {
        return type;
    }

    @JsonSetter("enotify_type")
    public DoeEnotify setType(AceEnotifyOptions.Type type) {
        this.type = type;
        return this;
    }

    @JsonGetter("title")
    public String getTitle() {
        return title;
    }

    @JsonSetter("enotify_title")
    public DoeEnotify setTitle(String title) {
        this.title = title;
        return this;
    }

    public URI getLink() {
        return link;
    }

    public DoeEnotify setLink(URI link) {
        this.link = link;
        return this;
    }

    @JsonGetter("createTime")
    @AceDate
    public ZonedDateTime getCreateTime() {
        return createTime;
    }

    @JsonSetter("create_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIMEZONE)
    public DoeEnotify setCreateTime(ZonedDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    @JsonGetter("startTime")
    @AceDate
    public ZonedDateTime getStartTime() {
        return startTime;
    }

    @JsonSetter(value = "start_time", nulls = Nulls.SKIP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIMEZONE)
    public DoeEnotify setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    @JsonGetter("endTime")
    @AceDate
    public ZonedDateTime getEndTime() {
        return endTime;
    }

    @JsonSetter(value = "end_time", nulls = Nulls.SKIP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = TIMEZONE)
    public DoeEnotify setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public static class DoeEnotifies {
        private DoeEnotifiesData data;

        public DoeEnotifiesData getData() {
            return data;
        }

        public DoeEnotifies setData(DoeEnotifiesData data) {
            this.data = data;
            return this;
        }
    }

    public static class DoeEnotifiesData {

        private List<DoeEnotify> value;

        public List<DoeEnotify> getValue() {
            return value;
        }

        public DoeEnotifiesData setValue(List<DoeEnotify> value) {
            this.value = value;
            return this;
        }
    }
}
