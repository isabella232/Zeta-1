package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.dao.ZetaEmailUnsubscribeRepository;
import com.ebay.dss.zds.model.ZetaEmailUnsubscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/mail")
public class MailController {

    @Autowired
    private ZetaEmailUnsubscribeRepository zetaEmailUnsubscribeRepository;


    @RequestMapping(value = "/unsubscribe", method = RequestMethod.GET)
    public String unsubscribe(String nt, String unsubscribeId) {
        ZetaEmailUnsubscribe zetaEmailUnsubscribe = new ZetaEmailUnsubscribe();
        zetaEmailUnsubscribe.setNt(nt);
        zetaEmailUnsubscribe.setUnsubscribeId(unsubscribeId);
        zetaEmailUnsubscribe.setCreateTime(new Date());
        zetaEmailUnsubscribeRepository.save(zetaEmailUnsubscribe);
        return "Unsubscribe Successfully!";
    }

    @RequestMapping(value = "/subscribe", method = RequestMethod.GET)
    public String subscribe(String subscribeId) {
        if (zetaEmailUnsubscribeRepository.existsById(subscribeId)) {
            zetaEmailUnsubscribeRepository.deleteById(subscribeId);
        }
        return "Subscribe Successfully!";
    }

    @RequestMapping(value = "/unsubscribelist", method = RequestMethod.GET)
    public List<ZetaEmailUnsubscribe> getUnsubscribeList(String nt) {
        return zetaEmailUnsubscribeRepository.findByNt(nt);
    }

}
