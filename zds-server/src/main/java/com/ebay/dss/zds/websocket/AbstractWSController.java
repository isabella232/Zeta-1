package com.ebay.dss.zds.websocket;

import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.InterpreterExecutionException;
import com.ebay.dss.zds.exception.InterpreterServiceException;
import com.ebay.dss.zds.rest.error.match.ExceptionRuleService;
import com.ebay.dss.zds.message.EventTracker;
import com.ebay.dss.zds.rest.error.ErrorDTO;
import com.ebay.dss.zds.rest.error.handler.InterpreterExceptionSupport;
import com.ebay.dss.zds.websocket.notebook.WSConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpAttributes;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.security.Principal;
import java.util.Map;

import static com.ebay.dss.zds.message.event.Event.ZetaConnectionSocketSentEvent;

/**
 * Created by wenliu2 on 4/11/18.
 */
public abstract class AbstractWSController extends InterpreterExceptionSupport {
    private final Logger logger = LoggerFactory.getLogger(AbstractWSController.class);
    private final SimpMessagingTemplate template;

    public AbstractWSController(SimpMessagingTemplate template,
                                ExceptionRuleService ruleService) {
        super(ruleService);
        this.template = template;

        /**Register this obj in the central context reference set**/
        ServerContextReference.register(this);
    }

    public Logger getLogger() {
        return this.logger;
    }

    public String getTargetTopic() {
        return WSConstants.targetTopic;
    }

    public String getQueue() {
        return WSConstants.queue;
    }

    public SimpMessagingTemplate getTemplate() {
        return this.template;
    }

    @MessageExceptionHandler(value = {Exception.class})
    public void handleMessageException(Exception ex, Message message, MessageHeaders mHeaders, Principal principal) {
        WebSocketResp<ErrorDTO> resp = ruleMatch(ex, ErrorCode.UNKNOWN_EXCEPTION, WebSocketResp.OP.INTERNAL_ERROR);
        convertAndSendToUser(principal.getName(), resp, null);
    }

    @MessageExceptionHandler(value = {InterpreterServiceException.class})
    public void handleInterpreterConnectionException(InterpreterServiceException ise, Message<?> message, MessageHeaders mHeaders, Principal principal) {
        WebSocketResp<ErrorDTO> resp = handleInterpreterConnectionException(ise);
        convertAndSendToUser(principal.getName(), resp, null);
        EventTracker.postEvent(ZetaConnectionSocketSentEvent(principal.getName(), ise.getNoteId(), "", resp));
    }

    @MessageExceptionHandler(value = {InterpreterExecutionException.class})
    public void handleInterpreterExecutionException(InterpreterExecutionException iee, Message<?> message, MessageHeaders mHeaders, Principal principal) {
        WebSocketResp<ErrorDTO> resp = handleInterpreterExecutionException(iee);
        convertAndSendToUser(principal.getName(), resp, null);
    }

    public void convertAndSendToUser(String user, WebSocketResp<?> payload, @Nullable Map<String, Object> headers) throws MessagingException {
        this.getTemplate().convertAndSendToUser(user, getQueue(), payload, headers);
    }

    private void runWithExceptionHandler(Principal principal, Runnable runnable) {
        try {
            runnable.run();
        } catch (InterpreterExecutionException iee) {
            handleInterpreterExecutionException(iee, null, null, principal);
        } catch (InterpreterServiceException ise) {
            handleInterpreterConnectionException(ise, null, null, principal);
        } catch (Exception ex) {
            handleMessageException(ex, null, null, principal);
        }
    }

    // this must be execute in the thread that has available security context
    public Runnable attachCurrentSecurityContext(Principal principal, Runnable runnable) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        SimpAttributes simpAttributes = SimpAttributesContextHolder.getAttributes();
        return () -> {
            try {
                SecurityContextHolder.setContext(securityContext);
                RequestContextHolder.setRequestAttributes(attributes);
                SimpAttributesContextHolder.setAttributes(simpAttributes);
                runWithExceptionHandler(principal, runnable);
            } finally {
                SecurityContextHolder.clearContext();
                RequestContextHolder.resetRequestAttributes();
                SimpAttributesContextHolder.resetAttributes();
            }
        };
    }

}
