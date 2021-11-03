package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.model.ReleaseHistory;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.service.ReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by zhouhuang on 2019/3/8.
 */
@RequestMapping("/Release")
@RestController
public class ReleaseController {

    @Autowired
    private ReleaseService releaseService;

    @RequestMapping(path = "/checkStatus", method = RequestMethod.GET)
    public Object checkStatus(String releaseTag, String issueName, @AuthenticationNT String nt) {
        return releaseService.checkJira(nt, releaseTag, issueName);
    }

    @RequestMapping(path = "/status/{releaseTag}", method = RequestMethod.GET)
    public Object getStatus(@AuthenticationNT String nt, @PathVariable("releaseTag") String releaseTag) {
        return releaseService.getStatus(nt, releaseTag);
    }

    @RequestMapping(path = "/history", method = RequestMethod.GET)
    public List<ReleaseHistory> getHistory(@AuthenticationNT String nt) {
        return releaseService.getHistory(nt);
    }

    @RequestMapping(path = "/manifest",method = RequestMethod.POST)
    public Object manifest(@RequestBody Object req){
        return releaseService.manifest(req);
    }

    @RequestMapping(path = "/sendValidateMail",method = RequestMethod.POST)
    public Object sendValidateMail(@RequestBody Object req){
        return releaseService.sendValidateMail(req);
    }

    @RequestMapping(path = "/rollout",method = RequestMethod.POST)
    public Object rollout(@RequestBody Object req){
        return releaseService.rollout(req);
    }
}
