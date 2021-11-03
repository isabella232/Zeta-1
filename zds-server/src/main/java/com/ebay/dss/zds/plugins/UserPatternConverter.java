package com.ebay.dss.zds.plugins;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

@Plugin(name = "userPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys("user")
public class UserPatternConverter extends LogEventPatternConverter {

    private UserPatternConverter(String name, String style) {
        super(name, style);
    }

    public static UserPatternConverter newInstance(final String[] options) {
        return new UserPatternConverter("user", null);
    }

    @Override
    public void format(LogEvent logEvent, StringBuilder stringBuilder) {
        stringBuilder.append(getUser());
    }

    static String getUser() {
        String value = "-";
        SecurityContext context = SecurityContextHolder.getContext();
        if (Objects.nonNull(context)) {
            Authentication auth = context.getAuthentication();
            if (Objects.nonNull(auth)) {
                value = auth.getName();
            }
        }
        return value;
    }
}
