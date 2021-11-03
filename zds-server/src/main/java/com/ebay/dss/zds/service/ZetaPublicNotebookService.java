package com.ebay.dss.zds.service;

import com.ebay.dss.zds.model.ZetaNotebook;

import java.util.List;

public interface ZetaPublicNotebookService {

    public ZetaNotebook createPublicNotebook(String notebookId, String owner);

    /**
     * Copy a public notebook from repo
     * @param referredNotebookId notebook to be referred
     * @param referNt this must not be the notebook owner
     * @return
     */
    public ZetaNotebook createPublicNotebookRef(String referredNotebookId, String referNt);

    /**
     * Get a public notebook from repo
     * @param referredNotebookId notebook id
     * @param referNt this must not be the notebook owner
     * @return
     */
    public ZetaNotebook findPublicNotebook(String referredNotebookId, String referNt);

    public List<ZetaNotebook> findAllPublicNotebook(Integer limit);

}
