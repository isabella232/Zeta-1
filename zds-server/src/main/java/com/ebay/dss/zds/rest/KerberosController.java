package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.kerberos.KerberosContext;
import com.ebay.dss.zds.kerberos.KerberosContextUser;
import com.ebay.dss.zds.service.KerberosContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/krb")
public class KerberosController {

    private final static Logger log = LoggerFactory.getLogger(KerberosController.class);
    private final KerberosContextService kerberosContextService;

    public KerberosController(KerberosContextService kerberosContextService) {
        this.kerberosContextService = kerberosContextService;
    }

    @GetMapping("/show")
    public ResponseEntity<?> show(@RequestParam String key) throws Exception {
        log.info("Show kerberos context status of {}.", key);
        KerberosContextUser user = kerberosContextService.getUser(key);
        KerberosContext kc = kerberosContextService.getContext(key);
        if (user != null && kc != null) {
            return ResponseEntity.ok(user.toString() + "\n" + kc.toString());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> testKrb(@RequestParam String key) throws Exception {
        log.info("Test whether kerberos context of {} is valid.", key);
        KerberosContext kc = kerberosContextService.getContext(key);
        if (kc != null) {
            kc.doKerberosLogin();
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/invalid")
    public ResponseEntity<?> invalidKrb(@RequestParam String key) {
        log.info("Close kerberos context of {}.", key);
        KerberosContext kc = kerberosContextService.getContext(key);
        if (kc != null) {
            kc.close();
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
