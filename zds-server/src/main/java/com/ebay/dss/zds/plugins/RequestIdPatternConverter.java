package com.ebay.dss.zds.plugins;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.springframework.messaging.simp.SimpAttributes;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.UUID;

import static com.ebay.dss.zds.common.Constant.ZDS_SERVER_REQUEST_ID_ATTR;

@Plugin(name = "requestIdPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys("requestId")
public final class RequestIdPatternConverter extends LogEventPatternConverter {

    private RequestIdPatternConverter(String name, String style) {
        super(name, style);
    }

    public static RequestIdPatternConverter newInstance(final String[] options) {
        return new RequestIdPatternConverter("requestId", null);
    }

    @Override
    public void format(LogEvent logEvent, StringBuilder stringBuilder) {
        stringBuilder.append(getRequestId());
    }

    static String getRequestId() {
        String value = "-";
        String maybeRequestId = tryGetRequestId();
        if (StringUtils.isNotBlank(maybeRequestId)) {
            value = maybeRequestId;
        }
        maybeRequestId = tryGetSimpRequestId();
        if (StringUtils.isNotBlank(maybeRequestId)) {
            value = maybeRequestId;
        }
        return value;
    }

    private static String tryGetRequestId() {

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object maybeRequestId = attributes.getAttribute(ZDS_SERVER_REQUEST_ID_ATTR, RequestAttributes.SCOPE_SESSION);
            if (maybeRequestId instanceof UUID) {
                return maybeRequestId.toString();
            }
        }
        return null;
    }

    private static String tryGetSimpRequestId() {
        SimpAttributes simpAttributes = SimpAttributesContextHolder.getAttributes();
        if (simpAttributes != null) {
            return simpAttributes.getSessionId();
        }
        return null;
    }
}
