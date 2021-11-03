package com.ebay.dss.zds.rest.error.match;

import com.ebay.dss.zds.rest.error.ErrorRow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ErrorLogger {
    Logger log = LogManager.getLogger(ErrorLogger.class);
    static void log(List<ErrorRow> errorRows, Throwable t) {
        Map<String, Object> messageComponents = new HashMap<>();
        messageComponents.put("errorRows", errorRows);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos);
        t.printStackTrace(printStream);
        messageComponents.put("stackTrace", baos.toString());

        ObjectMessage message = new ObjectMessage(messageComponents);
        log.error(message);
    }
}
