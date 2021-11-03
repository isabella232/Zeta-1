package com.ebay.dss.zds.model;


import java.io.InputStream;

public class ZetaAttachmentContent {

    private Long id;
    private InputStream inputStream;

    public Long getId() {
        return id;
    }

    public ZetaAttachmentContent setId(Long id) {
        this.id = id;
        return this;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public ZetaAttachmentContent setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

}
