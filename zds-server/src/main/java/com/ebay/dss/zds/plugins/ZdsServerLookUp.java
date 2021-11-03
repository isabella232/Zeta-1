package com.ebay.dss.zds.plugins;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Plugin(name = "zds", category = StrLookup.CATEGORY)
public class ZdsServerLookUp implements StrLookup {

    @Override
    public String lookup(String key) {
        switch (key) {
            case "logTime":
                return getLogTime();
            case "user":
                return UserPatternConverter.getUser();
            case "requestId":
                return RequestIdPatternConverter.getRequestId();
        }
        return getLogTime();
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }

    private String getLogTime() {
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
