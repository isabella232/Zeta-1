package com.ebay.dss.zds.event.listener;

import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.event.events.NoteMetaUpdateEvent;
import com.ebay.dss.zds.exception.NotebookException;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.model.notebook.meta.NotebookMeta;
import com.ebay.dss.zds.service.DoeESService;
import com.ebay.dss.zds.service.ZetaNotebookMetaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NoteMetaUpdateListener implements ApplicationListener<NoteMetaUpdateEvent> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(NoteMetaUpdateListener.class);

    @Autowired
    DoeESService doeESService;

    @Autowired
    ZetaNotebookMetaService zetaNotebookMetaService;

    @Autowired
    private ZetaNotebookRepository zetaNotebookRepository;

    @Async("asyncEventExecutor")
    @Override
    public void onApplicationEvent(NoteMetaUpdateEvent event) {
        String id = event.getId();
        boolean buildRefs = event.isBuildRefs();
        NotebookMeta meta = zetaNotebookMetaService.getNotebookMetaById(id);
        if (meta == null) {
            LOGGER.warn("Cannot find notebook meta, id is:" + id);
            return;
        }
        if (meta.getIsPublic() != null && meta.getIsPublic().equals("1")) {
            if (buildRefs) {
                meta = buildReference(id);
                updateMeta(meta);
            }
            doeESService.updateDOEMeta(id);
        } else {

            doeESService.deleteDOEMeta(id);
        }


    }

    private NotebookMeta updateMeta(NotebookMeta input) {
        return zetaNotebookMetaService.createOrUpdateNotebookMeta(input);
    }

    private NotebookMeta buildReference(String id) {
        ZetaNotebook notebook = zetaNotebookRepository.getNotebook(id);
        return buildReference(notebook);
    }

    private NotebookMeta buildReference(ZetaNotebook notebook) {
        if (!zetaNotebookMetaService.hasMeta(notebook.getId())) {
            return null;
        }
        String refs = zetaNotebookMetaService.parseNotebookReference(notebook);
        NotebookMeta meta = zetaNotebookMetaService.getNotebookMetaById(notebook.getId());
        meta.setReference(refs);
        return meta;
    }
}
