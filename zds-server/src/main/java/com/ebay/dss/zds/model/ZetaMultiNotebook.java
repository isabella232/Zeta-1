package com.ebay.dss.zds.model;

import java.util.Collection;

public class ZetaMultiNotebook extends ZetaNotebook {

    private Collection<ZetaNotebook> subNotebooks;

    public Collection<ZetaNotebook> getSubNotebooks() {
        return subNotebooks;
    }

    public void setSubNotebooks(Collection<ZetaNotebook> subNotebooks) {
        this.subNotebooks = subNotebooks;
    }

    public ZetaMultiNotebook(ZetaNotebook notebook) {
        this.setContent(notebook.getContent());
        this.setNt(notebook.getContent());
    }

    public ZetaMultiNotebook() {
    }
}
