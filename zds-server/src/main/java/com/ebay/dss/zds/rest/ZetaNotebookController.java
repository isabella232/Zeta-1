package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.common.ZipSqlFile;
import com.ebay.dss.zds.dao.ZetaFavoriteRepository;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.event.events.NoteMetaDeleteEvent;
import com.ebay.dss.zds.event.events.NoteMetaUpdateEvent;
import com.ebay.dss.zds.event.events.NoteMetasDeleteEvent;
import com.ebay.dss.zds.exception.ApplicationBaseException;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.NotebookException;
import com.ebay.dss.zds.exception.NotebookExistException;
import com.ebay.dss.zds.interpreter.InterpreterManager;
import com.ebay.dss.zds.model.*;
import com.ebay.dss.zds.model.notebook.meta.NotebookMeta;
import com.ebay.dss.zds.packages.PackagesSubject;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.rest.wrapper.IntWrapper;
import com.ebay.dss.zds.service.ZetaNotebookMetaService;
import com.ebay.dss.zds.service.ZetaNotebookService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

import static com.ebay.dss.zds.interpreter.InterpreterConfiguration.DEFAULT_DYNAMIC_CONFIG_OBJECT_VIEW;
import static com.ebay.dss.zds.interpreter.InterpreterConfiguration.DEFAULT_REFERENCE_KEY;

@RestController
@EnableAsync
@RequestMapping("/notebooks")
public class ZetaNotebookController {

    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ZetaNotebookRepository zetaNotebookRepository;

    @Autowired
    private ZetaNotebookMetaService zetaNotebookMetaService;

    @Autowired
    private ZetaFavoriteRepository zetaFavoriteRepository;

    @Autowired
    private ZetaNotebookService zetaNotebookService;

    @Autowired
    private InterpreterManager interpreterManager;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ZetaResponse<ZetaNotebook> getZetaNotebook(
            @AuthenticationNT String nt,
            @PathVariable("id") String id,
            @RequestParam(defaultValue = "0") Integer opened) throws Exception {
        ZetaNotebook note = DEFAULT_REFERENCE_KEY.equals(id) ? DEFAULT_DYNAMIC_CONFIG_OBJECT_VIEW
                : zetaNotebookService.stateOpenedAndGetNotebook(id, nt, opened);
        if (interpreterManager.isConnected(id, nt)) {
            note.setStatus(ZetaStatus.CONNECTED.getStatus());
        }
        return new ZetaResponse<>(note, HttpStatus.OK);
    }
    @RequestMapping(value = "/{id}/readOnly", method = RequestMethod.GET)
    public ZetaResponse<ZetaNotebook> getReadonlyZetaNotebook(
            @AuthenticationNT String nt,
            @PathVariable("id") String id) throws Exception {
        ZetaNotebook note = zetaNotebookService.getReadOnlyZetaNotebook(id);
        if (interpreterManager.isConnected(id, nt)) {
            note.setStatus(ZetaStatus.CONNECTED.getStatus());
        }
        return new ZetaResponse<>(note, HttpStatus.OK);
    }
    @RequestMapping(value = "/{id}/clone", method = RequestMethod.GET)
    public ZetaResponse<ZetaNotebook> getZetaNotebookCopy(
            @PathVariable("id") String id) {
        ZetaNotebook note = zetaNotebookRepository.getNotebook(id, 0);
        return new ZetaResponse<>(note, HttpStatus.OK);
    }

    @RequestMapping(value = "/nt/{nt}", method = RequestMethod.GET)
    public ZetaResponse getZetaNotebookByNt(
            @AuthenticationNT String nt) {
        List<ZetaNotebook> notes = zetaNotebookService.getZetaNotebookBriefs(nt);
        //append default configuration file with other notebooks + configuration files
        notes.add(DEFAULT_DYNAMIC_CONFIG_OBJECT_VIEW);
        List<String> connectedId = interpreterManager.getConnectedNotebookIdByUserName(nt);
        appendStatus(notes, connectedId);
        return new ZetaResponse<>(notes, HttpStatus.OK);
    }

    @RequestMapping(value = "/opened", method = RequestMethod.GET)
    public ZetaResponse getOpenedZetaNotebookByNt(@AuthenticationNT String nt) {
        List<ZetaNotebook> notes = zetaNotebookService.getOpenedZetaNotebooks(nt);
        List<String> connectedId = interpreterManager.getConnectedNotebookIdByUserName(nt);
        appendStatus(notes, connectedId);
        return new ZetaResponse<>(notes, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ZetaResponse getAllZetaNotebooksByNt(
            @AuthenticationNT String nt) {
        return getZetaNotebookByNt(nt);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ZetaResponse<ZetaNotebook> addZetaNotebook(
            @AuthenticationNT String nt,
            @RequestBody ZetaNotebook notebook) {
        notebook.setNt(nt);
        if (zetaNotebookRepository.hasNotebook(notebook)) {
            throw new NotebookExistException("Notebook `" + notebook.getTitle() + "` Already Exist");
        }
        ZetaNotebook book = zetaNotebookRepository.addNotebook(notebook);
        return new ZetaResponse<>(book, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public ZetaResponse<ZetaNotebook> updateNotebook(
        @AuthenticationNT String nt,
        @RequestBody ZetaNotebook notebook) {
        notebook.setNt(nt);
        if (zetaNotebookRepository.isNotebookTitleExist(notebook)) {
            throw new NotebookExistException("Notebook `" + notebook.getTitle() + "` Already Exist");
        }
        ZetaNotebook book = zetaNotebookRepository.updateNotebook(notebook);
        applicationEventPublisher.publishEvent(new NoteMetaUpdateEvent(this, notebook.getId(), true));
        return new ZetaResponse<>(book, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/move", method = RequestMethod.PUT)
    public ZetaResponse<ZetaNotebook> moveNotebook(
        @AuthenticationNT String nt,
        @RequestBody ZetaNotebook notebook) {
        notebook.setNt(nt);
        if (zetaNotebookRepository.isNotebookTitleExist(notebook)) {
            throw new NotebookExistException("Notebook `" + notebook.getTitle() + "` Already Exist");
        }
        ZetaNotebook book = zetaNotebookRepository.moveNotebook(notebook);
        applicationEventPublisher.publishEvent(new NoteMetaUpdateEvent(this, notebook.getId()));
        return new ZetaResponse<>(book, HttpStatus.OK);
    }

    @RequestMapping(value = "/closed/{id}", method = RequestMethod.PUT)
    public ZetaResponse updateNotebookOpenedStatusByIdAsClose(@PathVariable("id") String id, @AuthenticationNT String nt) {
        zetaNotebookRepository.updateNotebookOpenedStatusByIdAsClose(nt, id);
        return new ZetaResponse<>("Notebook Closed", HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/status/{status}", method = RequestMethod.PUT)
    public ZetaResponse updateNotebookState(
            @AuthenticationNT String nt,
            @PathVariable("id") String id,
            @PathVariable("status") String state) {
        int res = zetaNotebookRepository.updateNotebookStateByIdAndNt(id, state, nt);
        return new ZetaResponse<>(new IntWrapper(res), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/preference", method = RequestMethod.PUT)
    public ZetaResponse updateNotebookPreferenceById(
            @AuthenticationNT String nt,
            @PathVariable("id") String id,
            @RequestBody String preference) {
        int res = zetaNotebookRepository.updateNotebookPreferenceByIdAndNt(id, preference, nt);
        return new ZetaResponse<>(new IntWrapper(res), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/seq/{seq}", method = RequestMethod.PUT)
    public ZetaResponse updateNotebookSeqById(@PathVariable("id") String id, @PathVariable("seq") int seq, @AuthenticationNT String nt) {
        zetaNotebookRepository.updateNotebookSeqById(nt, id, seq);
        return new ZetaResponse<>("Updated", HttpStatus.OK);
    }
    @RequestMapping(value = "/seqs", method = RequestMethod.PUT)
    public ZetaResponse updateNotebookSeqById(@AuthenticationNT String nt, @RequestBody Map<String, Integer> seqMap) {
        zetaNotebookService.updateNotebookSeqs(seqMap, nt);
        return new ZetaResponse<>("Updated", HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/packages", method = RequestMethod.PUT)
    public ZetaResponse applyPackages(
        @AuthenticationNT String nt,
        @PathVariable("id") String id,
        @RequestBody PackagesSubject.PackageValue packages) {
      PackagesSubject.packagesSubject.setPackages(new PackagesSubject.SubjectKey(nt, id), packages);
      return new ZetaResponse<>("Applyed", HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/history", method = RequestMethod.GET)
    public ZetaResponse getNotebookHistory(
            @AuthenticationNT String nt,
            @PathVariable("id") String id) {
        List<Map<String, Object>> result = zetaNotebookRepository.getNotebookHistoryByIdAndNt(id, nt);
        return new ZetaResponse<>(result, HttpStatus.OK);
    }
    @RequestMapping(value = "/{id}/multinotebook/history", method = RequestMethod.GET)
    public ZetaResponse getMultiNotebookHistory(
            @AuthenticationNT String nt,
            @PathVariable("id") String id) {
        List<Map<String, Object>> result = zetaNotebookService.getMultiNotebookHistory(id);
        return new ZetaResponse<>(result, HttpStatus.OK);
    }
    @RequestMapping(value = "/{requestid}/reqhistory", method = RequestMethod.GET)
    public ZetaResponse getNotebookRequestHistory(
            @PathVariable("requestid") String requestid) {
        List<Map<String, Object>> result = zetaNotebookRepository.getNotebookHistoryByRequestId(requestid);
        return new ZetaResponse<>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ZetaResponse deleteNotebook(
            @AuthenticationNT String nt,
            @PathVariable("id") String id) {
        int res = zetaNotebookService.deleteZetaNotebook(id, nt);
        applicationEventPublisher.publishEvent(new NoteMetaDeleteEvent(this, id));
        return new ZetaResponse<>(new IntWrapper(res), HttpStatus.OK);
    }

    @PutMapping("/{id}/{type}/favorite")
    public ZetaResponse favoriteNotebook(
        @AuthenticationNT String nt,
        @PathVariable("id") String id,
        @PathVariable("type") String type) {
        zetaFavoriteRepository.favoriteNote(id, nt, type);
        return new ZetaResponse<>("favorite", HttpStatus.OK);
    }

    @PutMapping("/{id}/{type}/unFavorite")
    public ZetaResponse unFavoriteNotebook(
        @AuthenticationNT String nt,
        @PathVariable("id") String id,
        @PathVariable("type") String type) {
        zetaFavoriteRepository.unFavoriteNote(id, nt, type);
        return new ZetaResponse<>("unFavorite", HttpStatus.OK);
    }

    @GetMapping("/favorite")
    public ZetaResponse favoriteList(@AuthenticationNT String nt) {
        List<ZetaFavorite> favorites = zetaFavoriteRepository.findAllByNtAndFavoriteIs(nt, "1");
        return new ZetaResponse<>(favorites, HttpStatus.OK);
    }
    @GetMapping("/favoriteShareNotebook")
    public ZetaResponse favoriteShareNotebook(@AuthenticationNT String nt) {
        List<ZetaNotebook> favorites = zetaNotebookRepository.getFavoriteShareNotebook(nt);
        List<ZetaFavorite> zplIds = zetaFavoriteRepository.findAllByNtAndFavoriteAndFavoriteTypeIs(nt, "1", "share_zpl_nb");
        Map<String, Object> result = new HashMap<>();
        result.put("zetaFavorite", favorites);
        result.put("zeppelinFavorite", zplIds);
        return new ZetaResponse<>(result, HttpStatus.OK);
    }

    @RequestMapping(value = "/downloadNotebook", method = RequestMethod.GET, produces = {"application/zip", MediaType.APPLICATION_JSON_UTF8_VALUE})
    public ResponseEntity<byte[]> downloadNotebooks(@RequestParam("ids") String ids) throws IOException {
        String[] notebookIds = ids.split("\\+", -1);

        List<Map<String, Object>> notebookList = zetaNotebookRepository.getNotebookContentByIds(notebookIds);

        /*append default configuration file with other notebooks + configuration files**/
        if (Arrays.toString(notebookIds).contains(DEFAULT_REFERENCE_KEY)) {
            notebookList.add(DEFAULT_DYNAMIC_CONFIG_OBJECT_VIEW.toMap());
        }

        List<SqlFile> fileInfo = new ArrayList<>();
        for (Map<String, Object> notebook : notebookList) {
            fileInfo.add(new SqlFile(notebook.get("title") + ".sql", (String) notebook.get("content")));
        }

        HttpHeaders headers = new HttpHeaders();

        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(System.currentTimeMillis() + ".zip").build());

        return new ResponseEntity<>(ZipSqlFile.getSqlFileZipStream(fileInfo), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/meta/{id}")
    public ZetaResponse getNotebookMeta(@PathVariable("id") String id) {
        NotebookMeta meta = zetaNotebookMetaService.getNotebookMetaById(id);
        if (meta == null) {
            throw new NotebookException("Cannot find notebook meta");
        }
        return new ZetaResponse<>(meta, HttpStatus.OK);
    }

    @PostMapping(value="/meta")
    public ZetaResponse createNotebookMeta(@AuthenticationNT String nt, @RequestBody NotebookMeta meta) {
        if (StringUtils.isEmpty(meta.getType())) {
            meta.setType("zeta");
        }
        meta = zetaNotebookMetaService.createOrUpdateNotebookMeta(meta);
        applicationEventPublisher.publishEvent(new NoteMetaUpdateEvent(this, meta.getId(), true));
        return new ZetaResponse<>(meta, HttpStatus.OK);
    }

    @PutMapping(value="/meta")
    public ZetaResponse updateNotebookMeta(@AuthenticationNT String nt, @RequestBody NotebookMeta meta) {
        if (StringUtils.isEmpty(meta.getType())) {
            meta.setType("zeta");
        }
        meta = zetaNotebookMetaService.createOrUpdateNotebookMeta(meta);
        applicationEventPublisher.publishEvent(new NoteMetaUpdateEvent(this, meta.getId()));
        return new ZetaResponse<>(meta, HttpStatus.OK);
    }


    @DeleteMapping(value = "/folders")
    public ZetaResponse deleteFolder(@AuthenticationNT String nt, @RequestBody DeleteFolderRequest folders) {
        List<String> res = zetaNotebookService.deleteFolders(nt, folders.getFolders(), folders.isRecursive());
        applicationEventPublisher.publishEvent(new NoteMetasDeleteEvent(this, res));
        return new ZetaResponse<>(res, HttpStatus.OK);
    }

    public <T> T appendStatus(List<T> notes, List<String> connectedId) {
        notes.forEach(note -> {
            if (note instanceof ZetaNotebook) {
                ZetaNotebook notebook = (ZetaNotebook) note;
                if (connectedId.contains(notebook.getId())) {
                    notebook.setStatus(ZetaStatus.CONNECTED.getStatus());
                }
            } else if (note instanceof Map) {
                Map<String, String> noteMap = (Map) note;
                if (connectedId.contains(noteMap.get("id"))) {
                    noteMap.put("status", ZetaStatus.CONNECTED.getStatus());
                }
            }
        });
        return null;
    }

    @GetMapping("/status/note/{id}")
    public ZetaResponse getNoteStatus(@PathVariable("id") String id,
                                      @AuthenticationNT String nt) {
        return new ZetaResponse<>(zetaNotebookService.getCurrentNoteRunningStatus(nt, id), HttpStatus.OK);
    }

    @GetMapping("/status/clear/note/{id}")
    public ZetaResponse clearNoteStatus(@PathVariable("id") String id,
                                        @AuthenticationNT String nt,
                                        @RequestParam(required = false) Long statementId) {
        if (statementId != null) {
            zetaNotebookService.cleanCurrentNoteStatus(id, statementId);
            return new ZetaResponse<>(String.format("Note: %s statement: %s status cleared", id, statementId), HttpStatus.OK);
        } else {
            zetaNotebookService.cleanCurrentNoteStatus(id);
            return new ZetaResponse<>(String.format("Note: %s status cleared", id), HttpStatus.OK);
        }
    }

    @GetMapping("/parse/notebook/{id}")
    public ZetaResponse parseNotebookSQL(@PathVariable("id") String id,
                                        @AuthenticationNT String nt) {
        return new ZetaResponse<>(zetaNotebookService.parseAndReplaceSQL(id), HttpStatus.OK);
    }

}
