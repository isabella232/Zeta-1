package com.ebay.dss.zds.message.sink;

import com.ebay.dss.zds.service.MailService;
import com.ebay.dss.zds.message.ZetaEventListener;
import com.ebay.dss.zds.message.TaggedEvent;
import com.ebay.dss.zds.message.ZetaEvent;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tatian on 2019-06-11.
 */
public class EmailEventSink implements ZetaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(EmailEventSink.class);

    MailService mailService;

    public EmailEventSink(MailService mailService) {
        this.mailService = mailService;
    }

    public void onEventReceived(ZetaEvent zetaEvent) {
        sendEmail(zetaEvent);
    }

    private void sendEmail(ZetaEvent zetaEvent){
        if (zetaEvent instanceof TaggedEvent.EmailStorable) {
            String eventJson = zetaEvent.toJson();
            TaggedEvent.EmailStorable monitorEvent = (TaggedEvent.EmailStorable) zetaEvent;
            String receiver = monitorEvent.getReceiver();
            if (StringUtils.isNotEmpty(receiver)) {
                String subject = monitorEvent.getSubject();
                String content = monitorEvent.getContent();
                boolean success = mailService.sendEmail(MailService.MailTemplate.TEXT, subject, content, receiver);
                if (success) {
                    logger.info("Succeeded to send email for event: " + eventJson);
                } else logger.error("Failed to send email for event: " + eventJson);
            } else logger.error("(No receiver specified) Failed to send email for event: " + eventJson);
        }
    }

}
