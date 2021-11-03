package com.ebay.dss.zds.websocket;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class SimpEventListener {

    private static final Logger log = LoggerFactory.getLogger(SimpEventListener.class);
    private final Map<String, SessionInfo> sessionInfoMap = new ConcurrentHashMap<>();

    public Collection<SessionInfo> getSessionListOfUser(String nt) {
        return sessionInfoMap.values()
                .stream()
                .filter(sessionInfo -> StringUtils.equalsIgnoreCase(nt, sessionInfo.getNt()))
                .collect(Collectors.toSet());
    }


    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        String sessionId = SimpAttributesContextHolder.currentAttributes().getSessionId();
        log.info("session connected {}, user {}", sessionId, event.getUser());
        if (event.getUser() != null) {
            sessionInfoMap.put(sessionId,
                    new SessionInfo()
                            .setNt(event.getUser().getName())
                            .setCreateTime(event.getTimestamp())
                            .setId(sessionId));
        }
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        String sessionId = SimpAttributesContextHolder.currentAttributes().getSessionId();
        log.info("session disconnected {}, user {}", sessionId, event.getUser());
        sessionInfoMap.remove(sessionId);
    }

    static class SessionInfo {
        private String nt;
        private String id;
        private long createTime;

        public String getNt() {
            return nt;
        }

        public SessionInfo setNt(String nt) {
            this.nt = nt;
            return this;
        }

        public String getId() {
            return id;
        }

        public SessionInfo setId(String id) {
            this.id = id;
            return this;
        }

        public long getCreateTime() {
            return createTime;
        }

        public SessionInfo setCreateTime(long createTime) {
            this.createTime = createTime;
            return this;
        }
    }
}
