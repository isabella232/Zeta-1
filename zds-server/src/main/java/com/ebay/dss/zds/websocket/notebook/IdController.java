package com.ebay.dss.zds.websocket.notebook;

import com.ebay.dss.zds.rest.error.match.ExceptionRuleService;
import com.ebay.dss.zds.websocket.AbstractWSController;
import com.ebay.dss.zds.websocket.SimpEventListener;
import com.ebay.dss.zds.websocket.WebSocketResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class IdController extends AbstractWSController {

    private static final Logger log = LoggerFactory.getLogger(IdController.class);
    private final SimpEventListener simpEventListener;

    public IdController(SimpMessagingTemplate template,
                        ExceptionRuleService ruleService,
                        SimpEventListener simpEventListener) {
        super(template, ruleService);
        this.simpEventListener = simpEventListener;
    }

    @MessageMapping("/id")
    public void id(StompHeaderAccessor accessor) {
        if (accessor != null && accessor.getSessionId() != null) {
            String id = accessor.getSessionId();
            if (getLogger().isTraceEnabled()) {
                log.trace("Current ws request id is {}", id);
            }
            SimpMessageHeaderAccessor ha = SimpMessageHeaderAccessor
                    .create(SimpMessageType.MESSAGE);
            ha.setSessionId(accessor.getSessionId());
            ha.setLeaveMutable(true);
            convertAndSendToUser(accessor.getSessionId(),
                    WebSocketResp.get(WebSocketResp.OP.GREETING, id),
                    getHeaders(accessor));
        }
    }

    @MessageMapping("/sessions")
    public void sessions(StompHeaderAccessor accessor, Principal principal) {
        if (accessor != null && accessor.getSessionId() != null) {
            convertAndSendToUser(accessor.getSessionId(),
                    WebSocketResp.get(WebSocketResp.OP.SESSIONS,
                            simpEventListener.getSessionListOfUser(principal.getName())),
                    getHeaders(accessor));
        }
    }

    private MessageHeaders getHeaders(StompHeaderAccessor accessor) {
        // this is a must for session based response
        SimpMessageHeaderAccessor ha = SimpMessageHeaderAccessor
                .create(SimpMessageType.MESSAGE);
        ha.setSessionId(accessor.getSessionId());
        ha.setLeaveMutable(true);
        return ha.getMessageHeaders();
    }

    @MessageMapping("/active")
    public void broadcastActive(StompHeaderAccessor accessor, Principal principal) {
        if (accessor != null && accessor.getSessionId() != null) {
            convertAndSendToUser(principal.getName(),
                    WebSocketResp.get(WebSocketResp.OP.SESSION_ACTIVE, accessor.getSessionId()),
                    null);
        }
    }

}
