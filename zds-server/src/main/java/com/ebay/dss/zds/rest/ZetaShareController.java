package com.ebay.dss.zds.rest;


import com.ebay.dss.zds.auth.ZetaUserDetails;
import com.ebay.dss.zds.auth.impl.jwt.JwtAuthenticationService;
import com.ebay.dss.zds.dao.ZetaDashboardRepository;
import com.ebay.dss.zds.dao.ZetaFavoriteRepository;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.dao.ZetaStatementRepository;
import com.ebay.dss.zds.exception.EntityNotFoundException;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.InvalidInputException;
import com.ebay.dss.zds.model.*;
import com.ebay.dss.zds.model.notebook.ZetaNotebookSummary;
import com.ebay.dss.zds.service.ZetaNotebookMetaService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/share")
public class ZetaShareController {

    @Autowired
    private ZetaStatementController zetaStatementController;

    @Autowired
    private JwtAuthenticationService jwtAuthenticationService;

    @Autowired
    private ZetaNotebookRepository zetaNotebookRepository;

    @Autowired
    private ZetaStatementRepository zetaStatementRepository;


    @Autowired
    private ZetaDashboardRepository zetaDashboardRepository;


    @Autowired
    private ZetaFavoriteRepository zetaFavoriteRepository;

    @Autowired
    private ZetaNotebookMetaService zetaNotebookMetaService;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ZetaResponse<ZetaNotebook> getZetaNotebook(
            @PathVariable("id") String id) {
        ZetaNotebook note = zetaNotebookRepository.getNotebook(id, null);

        return new ZetaResponse<>(note, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/history", method = RequestMethod.GET)
    public ZetaResponse getNotebookHistory(
            @PathVariable("id") String id) {
        List<Map<String, Object>> result = zetaNotebookRepository.getNotebookHistoryByIdAndNt(id);
        return new ZetaResponse<>(result, HttpStatus.OK);
    }


    @RequestMapping(value = "/statements/{id}", method = RequestMethod.GET)
    public ZetaResponse<ZetaStatement> getZetaStatement(
            @PathVariable("id") int id) {
        ZetaStatement zetaStatement = zetaStatementRepository.getZetaStatement(id);
        return new ZetaResponse<>(zetaStatement, HttpStatus.OK);
    }

    @RequestMapping(value = "/statements/multiget//{idlist}", method = RequestMethod.GET)
    public ZetaResponse<List<ZetaStatement>> getZetaStatementList(
            @PathVariable("idlist") String idList) {
        if (StringUtils.isEmpty(idList)) throw new InvalidInputException(ErrorCode.INVALID_INPUT, "id list is not set");
        List<Long> ids = new ArrayList<>();
        for (String strId : idList.split(",")) {
            try {
                ids.add(Long.parseLong(strId));
            } catch (NumberFormatException ne) {
                throw new InvalidInputException(ErrorCode.INVALID_INPUT, "id list should be a number list separated by ','");
            }
        }
        List<ZetaStatement> zetaStatements = zetaStatementRepository.getZetaStatements(ids);
        return new ZetaResponse<>(zetaStatements, HttpStatus.OK);
    }

    @RequestMapping(value = "/dashboard/{id}", method = RequestMethod.GET)
    public ZetaResponse<ZetaDashboard> get(@PathVariable("id") long id) {
        try {
            ZetaDashboard dashboard = zetaDashboardRepository.queryById(id);
            return new ZetaResponse<ZetaDashboard>(dashboard, HttpStatus.OK);
        } catch (EmptyResultDataAccessException e) {
            throw new EntityNotFoundException("dashboard not found");
        }
    }

    @GetMapping(value = "/favorite/{type}/{noteId}")
    public ZetaResponse favorite(@PathVariable("noteId") String noteId, @PathVariable("type") String favoriteType, @RequestParam String nt) {
        zetaFavoriteRepository.favoriteNote(noteId, nt, favoriteType);
        return new ZetaResponse<>("favorite", HttpStatus.OK);
    }

    @GetMapping(value = "/unfavorite/{type}/{noteId}")
    public ZetaResponse unfavorite(@PathVariable("noteId") String noteId, @PathVariable("type") String favoriteType, @RequestParam String nt) {
        zetaFavoriteRepository.unFavoriteNote(noteId, nt, favoriteType);
        return new ZetaResponse<>("unfavorite", HttpStatus.OK);
    }

    @GetMapping(value = "/favorite")
    public ZetaResponse unfavorite(@RequestParam String nt) {
        String[] types = new String[]{"share_nb", "share_zpl_nb", "schedule", "statement"};
        List<ZetaFavorite> favorites = zetaFavoriteRepository.findAllByNtAndFavoriteAndFavoriteTypeIn(nt, "1", Arrays.asList(types));
        return new ZetaResponse<>(favorites, HttpStatus.OK);
    }

    @GetMapping(value = "/openNotebookSummary")
    public ZetaResponse openNotebookSummary(@RequestParam List<String> ids) {
        List<ZetaNotebookSummary> smys = zetaNotebookMetaService.getNotebookSummaryByIds(ids);
        return new ZetaResponse<>(smys, HttpStatus.OK);
    }

    @GetMapping(value = "dumpFileProxy/{noteId}/{interpreter}/{requestId}", produces = "application/zip")
    public void authProxy(@RequestParam String token,
                          @PathVariable("requestId") int requestId,
                          @PathVariable("interpreter") String interpreter,
                          @PathVariable("noteId") String noteId,
                          HttpServletResponse response) throws IOException {
        ZetaUserDetails user = jwtAuthenticationService.authenticateAndGetDetails(token);
        zetaStatementController.getDumpResult(user.getUsername(), requestId, interpreter, noteId, response);
    }
}
