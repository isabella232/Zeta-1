package com.ebay.dss.zds.model.ace;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ListQuestionOptions {


    private String nt;
    private Integer page = 0;
    private Integer size = 10;
    private Scope scope = Scope.all;
    private boolean accepted = false;
    private boolean pickUp = false;
    private SortType sortType = SortType.updateTime;
    private ZonedDateTime startDatetime = ZonedDateTime.of(1970, 1, 1,
            0, 0, 0, 0, ZoneId.of("UTC"));

    public Integer getPage() {
        return page;
    }

    public ListQuestionOptions setPage(Integer page) {
        this.page = page;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public ListQuestionOptions setSize(Integer size) {
        this.size = size;
        return this;
    }

    public String getNt() {
        return nt;
    }

    public ListQuestionOptions setNt(String nt) {
        this.nt = nt;
        return this;
    }

    public Scope getScope() {
        return scope;
    }

    public ListQuestionOptions setScope(Scope scope) {
        this.scope = scope;
        return this;
    }

    public boolean isPickUp() {
        return pickUp;
    }

    public ListQuestionOptions setPickUp(boolean pickUp) {
        this.pickUp = pickUp;
        return this;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public ListQuestionOptions setAccepted(boolean accepted) {
        this.accepted = accepted;
        return this;
    }

    public SortType getSortType() {
        return sortType;
    }

    public ListQuestionOptions setSortType(SortType sortType) {
        this.sortType = sortType;
        return this;
    }

    public ZonedDateTime getStartDatetime() {
        return startDatetime;
    }

    public ListQuestionOptions setStartDatetime(ZonedDateTime startDatetime) {
        this.startDatetime = startDatetime;
        return this;
    }

    public enum Scope {
        all,
        self
    }

    public enum SortType {
        createTime,
        updateTime
    }
}
