package com.ebay.dss.zds.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "zeta_exception_rule")
@EntityListeners(AuditingEntityListener.class)
public class ExceptionRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "`order`")
    private int order;
    @Column
    private String filter;
    @Column
    private String message;
    @Column
    private boolean messageOnly;
    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "create_dt", updatable = false)
    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createDt;
    @Column(name = "update_dt")
    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime updateDt;

    @Column
    private String regex;

    public boolean isMessageOnly() {
        return messageOnly;
    }

    public ExceptionRule setMessageOnly(boolean messageOnly) {
        this.messageOnly = messageOnly;
        return this;
    }

    public String getRegex() {
        return regex;
    }

    public ExceptionRule setRegex(String regex) {
        this.regex = regex;
        return this;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public LocalDateTime getCreateDt() {
        return createDt;
    }

    public ExceptionRule setCreateDt(LocalDateTime createDt) {
        this.createDt = createDt;
        return this;
    }

    public LocalDateTime getUpdateDt() {
        return updateDt;
    }

    public ExceptionRule setUpdateDt(LocalDateTime updateDt) {
        this.updateDt = updateDt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExceptionRule that = (ExceptionRule) o;
        return id == that.id &&
                order == that.order &&
                Objects.equals(filter, that.filter) &&
                Objects.equals(message, that.message) &&
                Objects.equals(errorCode, that.errorCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, order, filter, message, errorCode);
    }

    @Override
    public String toString() {
        return "ExceptionRule{" +
                "id=" + id +
                ", order=" + order +
                ", filter='" + filter + '\'' +
                ", message='" + message + '\'' +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}
