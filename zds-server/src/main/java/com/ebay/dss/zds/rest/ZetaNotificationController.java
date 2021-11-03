package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.dao.ZetaNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tianrsun on 2018/7/19.
 */
@RestController
@RequestMapping("/notification")
public class ZetaNotificationController {

    @Autowired
    private ZetaNotificationRepository zetaNotificationRepository;

    @RequestMapping(method = RequestMethod.GET)
    public ZetaResponse getNotifications(){

        Map<String, Object> response = new HashMap<>();
        response.put("timeStamp",new Timestamp(new Date().getTime()));
        response.put("notifications",zetaNotificationRepository.getNotificationsStartedButNotEnd());
        return new ZetaResponse<>(response, HttpStatus.OK);
    }
}
