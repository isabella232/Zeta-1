package com.ebay.dss.zds.model;

import java.util.List;

public class ZetaAttachmentPage {

    private List<ZetaAttachment> attachments;
    private boolean sorted;
    private Long totalSize;
    private Integer totalPage;
    private Integer size;

    public List<ZetaAttachment> getAttachments() {
        return attachments;
    }

    public ZetaAttachmentPage setAttachments(List<ZetaAttachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    public boolean isSorted() {
        return sorted;
    }

    public ZetaAttachmentPage setSorted(boolean sorted) {
        this.sorted = sorted;
        return this;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public ZetaAttachmentPage setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
        return this;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public ZetaAttachmentPage setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public ZetaAttachmentPage setSize(Integer size) {
        this.size = size;
        return this;
    }
}
