package com.ebay.dss.zds.service;

import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.dao.ZetaPublicNotebookRepository;
import com.ebay.dss.zds.exception.EntityNotFoundException;
import com.ebay.dss.zds.exception.IllegalStatusException;
import com.ebay.dss.zds.model.ZetaNotebook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ZetaPublicNotebookServiceSimple implements ZetaPublicNotebookService {

    private final ZetaPublicNotebookRepository publicNotebookRepository;
    private final ZetaNotebookRepository notebookRepository;

    @Autowired
    public ZetaPublicNotebookServiceSimple(ZetaPublicNotebookRepository publicNotebookRepository,
                                           ZetaNotebookRepository notebookRepository) {
        this.publicNotebookRepository = publicNotebookRepository;
        this.notebookRepository = notebookRepository;
    }

    @Override
    @Transactional
    public ZetaNotebook createPublicNotebook(String notebookId, String owner) {
        Objects.requireNonNull(notebookId);
        Objects.requireNonNull(owner);
        int res = publicNotebookRepository.updateNotebookPublic(notebookId, owner);
        if (res != 1) {
            throw new IllegalStatusException("Notebook not found or is already public");
        }
        return notebookRepository.getNotebook(notebookId);
    }

    @Override
    @Transactional
    public ZetaNotebook createPublicNotebookRef(String referredNotebookId, String referNt) {
        Objects.requireNonNull(referredNotebookId);
        Objects.requireNonNull(referNt);
        ZetaNotebook referenceCheck = publicNotebookRepository.findPublicNotebook(referredNotebookId, referNt);
        if (Objects.nonNull(referenceCheck)) {
            throw new IllegalStatusException("Reference notebook is already created");
        }
        ZetaNotebook referred = notebookRepository.getNotebook(referredNotebookId);

        if (referred.getPublicRole() != ZetaNotebook.PublicRole.pub) {
            throw new IllegalStatusException("This is not a public notebook " + referredNotebookId);
        }

        if (referNt.equals(referred.getNt())) {
            String message = "This pub notebook belongs to requester, thus ref creation failed";
            log.error(message);
            throw new IllegalStatusException(message);
        }

        ZetaNotebook reference = createRefZetaNotebook(referred, referNt);

        try {
            reference = publicNotebookRepository.save(reference);
        } catch (DuplicateKeyException e1) {
            // if uuid collapse try one more time
            reference = publicNotebookRepository.save(genKey(reference));
        }

        return reference;
    }

    private static final Logger log = LogManager.getLogger();

    @Override
    public ZetaNotebook findPublicNotebook(String referredNotebookId, String referNt) {
        Objects.requireNonNull(referredNotebookId);
        Objects.requireNonNull(referNt);

        ZetaNotebook notebook = publicNotebookRepository.findPublicNotebook(referredNotebookId, referNt);
        if (Objects.isNull(notebook)) {
            String message = referredNotebookId + " reference does not exist for user " + referNt;
            log.error(message);
            throw new EntityNotFoundException(message);
        }
        return notebookRepository.getNotebook(notebook.getId());
    }

    public static final String PUBLIC_NOTE_BOOK_PATH = "/public_notebooks";

    private ZetaNotebook createRefZetaNotebook(ZetaNotebook referred, String referNt) {
        ZetaNotebook referenceNotebook = new ZetaNotebook();
        genKey(referenceNotebook);
        referenceNotebook.setNt(referNt);
        referenceNotebook.setPublicRole(ZetaNotebook.PublicRole.ref);
        referenceNotebook.setPublicReferred(referred.getId());
        referenceNotebook.setPreference(referred.getPreference());
        referenceNotebook.setTitle(referred.getTitle());
        referenceNotebook.setContent(referred.getContent());
        referenceNotebook.setUpdateDt(referred.getUpdateDt());
        referenceNotebook.setCreateDt(referred.getCreateDt());
        referenceNotebook.setLastRunDt(referred.getLastRunDt());
        referenceNotebook.setStatus(referred.getStatus());
        referenceNotebook.setPath(PUBLIC_NOTE_BOOK_PATH);
        referenceNotebook.setPlatform(referred.getPlatform());
        referenceNotebook.setProxyUser(referred.getProxyUser());
        referenceNotebook.setGitRepo(referred.getGitRepo());
        referenceNotebook.setSha(referred.getSha());
        referenceNotebook.setOpened(1);
        referenceNotebook.setSeq(-1);
        referenceNotebook.setNbType(referred.getNbType());
        referenceNotebook.setCollectionId(referred.getCollectionId());
        return referenceNotebook;
    }


    private ZetaNotebook genKey(ZetaNotebook notebook) {
        notebook.setId(UUID.randomUUID().toString());
        return notebook;
    }

    private static final int MAX_PUBLIC_NOTEBOOK_RETURNED = 50;

    /**
     * @param limit Currently useless
     * @return All public notebook
     */
    @Override
    public List<ZetaNotebook> findAllPublicNotebook(Integer limit) {
        return publicNotebookRepository.findAllPublicNotebook();
    }

}
