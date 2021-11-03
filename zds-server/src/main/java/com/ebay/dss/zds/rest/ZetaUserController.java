package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.exception.EntityNotFoundException;
import com.ebay.dss.zds.model.ZetaUser;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.service.BDPHTTPService;
import com.ebay.dss.zds.service.ZetaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * @author shighuang
 */
@RestController
@RequestMapping("/users")
public class ZetaUserController {

    private final ZetaUserService zetaUserService;

    private BDPHTTPService BDPHTTPService;

    @Autowired
    public ZetaUserController(ZetaUserService zetaUserService, BDPHTTPService BDPHTTPService) {
        this.zetaUserService = zetaUserService;
        this.BDPHTTPService = BDPHTTPService;
    }

    @GetMapping("/me")
    public ZetaUser find(@AuthenticationNT String nt) {
        return zetaUserService.getUser(nt);
    }

    @PutMapping(path = "/me")
    public ZetaUser save(@AuthenticationNT String nt, @RequestBody ZetaUser to) {
        to.setNt(nt);
        return zetaUserService.saveUserWithAllowedFields(to);
    }

    @GetMapping("/me/{cluster}")
    public List<String> getBatchAccount(@AuthenticationNT String nt, @PathVariable("cluster") String cluster) {
        List batchInfoList = BDPHTTPService.getUserBatchInfo(cluster, nt);
        if (Objects.nonNull(batchInfoList)) {
            return batchInfoList;
        } else {
            throw new EntityNotFoundException("Fail to get user batch info");
        }
    }
    @RequestMapping(value = "/verifyToken", method = RequestMethod.HEAD)
    public String verifyToken(@AuthenticationNT String nt) {
        return "";
    }
}
