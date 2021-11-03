package com.ebay.dss.zds.message.sink;

import com.ebay.dss.zds.common.QueueDestination;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.ZetaEventListener;
import com.ebay.dss.zds.message.event.ZetaCommonEvent.ZetaWebSocketContentEvent;
import com.ebay.dss.zds.websocket.notebook.CodeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tatian on 2019-07-01.
 */
public class WebSocketEventSink implements ZetaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventSink.class);

    CodeController codeController;

    public WebSocketEventSink(CodeController codeController) {
        this.codeController = codeController;
    }

    @Override
    public void onEventReceived(ZetaEvent zetaEvent) {
       if (zetaEvent instanceof ZetaWebSocketContentEvent) {
           ZetaWebSocketContentEvent webSocketContentEvent = (ZetaWebSocketContentEvent)zetaEvent;
           QueueDestination dest = new QueueDestination(webSocketContentEvent.getUserName(),
                   codeController.getTemplate(),
                   codeController.getQueue());
           dest.sendData(webSocketContentEvent.getRsp());
       }
    }
}
