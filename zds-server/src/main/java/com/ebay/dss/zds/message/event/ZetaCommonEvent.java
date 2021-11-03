package com.ebay.dss.zds.message.event;

import com.ebay.dss.zds.message.EventQueueIdentifier;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.websocket.WebSocketResp;
import com.google.gson.JsonObject;

import static com.ebay.dss.zds.common.JsonUtil.GSON;

/**
 * Created by tatian on 2019-07-02.
 */
public abstract class ZetaCommonEvent extends ZetaEvent {

    public EventQueueIdentifier getIdentifier() {
        return EventQueueIdentifier.COMMON;
    }

    public static class ZetaWebSocketContentEvent extends ZetaCommonEvent {
        protected String userName;
        protected WebSocketResp rsp;

        public ZetaWebSocketContentEvent(String userName, WebSocketResp rsp) {
            this.userName = userName;
            this.rsp = rsp;
        }

        @Override
        public JsonObject toJsonObject() {
            JsonObject base = new JsonObject();
            base.getAsJsonObject("eventContext").add("webSocketResp",
                    GSON.toJsonTree(rsp));
            return base;
        }

        public String getUserName() {
            return userName;
        }

        public WebSocketResp getRsp() {
            return rsp;
        }
    }
}
