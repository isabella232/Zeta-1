package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.service.ZetaPublicNotebookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pub_notebooks")
public class ZetaPublicNotebookController {

    private final ZetaPublicNotebookService publicNotebookService;

    @Autowired
    public ZetaPublicNotebookController(ZetaPublicNotebookService publicNotebookService) {
        this.publicNotebookService = publicNotebookService;
    }

    @GetMapping("/ref/{noteId}")
    public Object getPublicNotebookRef(@AuthenticationNT String nt, @PathVariable String noteId) {
        ZetaNotebook notebook = publicNotebookService.findPublicNotebook(noteId, nt);
        return ZetaResponse.ok(notebook);
    }

    @PostMapping("/ref/{noteId}")
    public Object createPublicNotebookRef(@AuthenticationNT String nt, @PathVariable String noteId) {
        // nt should be the owner of the notebook
        ZetaNotebook notebook = publicNotebookService.createPublicNotebookRef(noteId, nt);
        return ZetaResponse.ok(notebook); //.build();
    }

    @PostMapping("/{noteId}")
    public Object publishNotebook(@AuthenticationNT String nt, @PathVariable String noteId) {
        // nt should be the owner of the notebook
        publicNotebookService.createPublicNotebook(noteId, nt);
        return ZetaResponse.ok().build();
    }

    @GetMapping("/")
    public Object getPublicNotebookList(@RequestParam(required = false) Integer limit) {
        List<ZetaNotebook> notebooks = publicNotebookService.findAllPublicNotebook(limit);
        return ZetaResponse.ok(notebooks);
    }
}
