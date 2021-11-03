package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.dao.ZetaDashboardRepository;
import com.ebay.dss.zds.exception.EntityNotFoundException;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.InvalidInputException;
import com.ebay.dss.zds.exception.PermissionDenyException;
import com.ebay.dss.zds.model.ZetaDashboard;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.model.ZetaStatus;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author shighuang
 */
@RestController
@RequestMapping("/dashboard")
public class ZetaDashboardController {

    @Autowired
    private ZetaDashboardRepository repo;

    @RequestMapping(method = RequestMethod.POST)
    public ZetaResponse<ZetaDashboard> newDashboard(@AuthenticationNT String nt, @RequestBody ZetaDashboard req){
        if ( StringUtils.isEmpty(req.getName()) ){
            throw new InvalidInputException(ErrorCode.INVALID_INPUT, "name can't be empty.");
        }

        req.setNt(nt);
        repo.addDashboard(req);
        return new ZetaResponse<ZetaDashboard>(req, HttpStatus.OK);
    }

    @RequestMapping(value="/{id}", method = RequestMethod.GET)
    public ZetaResponse<ZetaDashboard> get(@AuthenticationNT String nt, @PathVariable("id") long id) {
        try {
            ZetaDashboard dashboard = repo.queryById(id);
            if (nt.equalsIgnoreCase(dashboard.getNt())) return new ZetaResponse<ZetaDashboard>(dashboard, HttpStatus.OK);
            throw new PermissionDenyException(ErrorCode.UNAUTHORIZED, "You have no permission to get this dashboard.");
        } catch (EmptyResultDataAccessException e){
            throw new EntityNotFoundException("dashboard not found");
        }
    }

    @RequestMapping(value="/{id}/layout", method = RequestMethod.PUT)
    public ZetaResponse<ZetaStatus> updateLayout(@AuthenticationNT String nt,
                                                 @PathVariable("id") long id,
                                                 @RequestBody ZetaDashboard dashboard) {
        try {
            ZetaDashboard queryDb = repo.queryById(id);
            if (nt.equalsIgnoreCase(queryDb.getNt())){
                repo.updateLayoutConfigById(id, dashboard.getLayoutConfig());
                return new ZetaResponse(HttpStatus.OK, ZetaStatus.SUCCESS);
            } else {
                throw new PermissionDenyException(ErrorCode.UNAUTHORIZED, "You have no permission to update this dashboard.");
            }
        }catch(EmptyResultDataAccessException e){
            throw new EntityNotFoundException("dashboard not found");
        }
    }

    @RequestMapping(method = RequestMethod.GET)
    public ZetaResponse<List<ZetaDashboard>> get(@AuthenticationNT String nt) {
        List<ZetaDashboard> dashboardList = repo.queryForUser(nt);
        return new ZetaResponse(dashboardList, HttpStatus.OK);
    }

    @RequestMapping(value="/{id}", method = RequestMethod.DELETE)
    public ZetaResponse<ZetaStatus> delete(@AuthenticationNT String nt, @PathVariable("id") long id) {
        ZetaDashboard dashboard = repo.queryById(id);
        if (nt.equalsIgnoreCase(dashboard.getNt())){
            repo.deleteById(id);
            return new ZetaResponse(HttpStatus.OK, ZetaStatus.SUCCESS);
        } else {
            throw new PermissionDenyException(ErrorCode.UNAUTHORIZED, "You have no permission to delete this dashboard.");
        }
    }
}

