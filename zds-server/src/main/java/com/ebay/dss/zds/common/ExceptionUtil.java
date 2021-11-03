package com.ebay.dss.zds.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tatian on 2019-05-14.
 */
public class ExceptionUtil {

    public static Throwable locate(Throwable ex, Class<? extends Throwable> target) {
        return locate(ex, target, true);
    }

    public static Throwable locate(Throwable ex, Class<? extends Throwable> target, boolean fromTop) {
        Throwable cause = ex;
        List<Throwable> targetList = new ArrayList<>();
        if (cause.getClass().equals(target)) {
            if (fromTop) return cause;
            else targetList.add(cause);
        }
        while(cause.getCause() != null) {
            cause = cause.getCause();
            if (cause.getClass().equals(target)) {
              if (fromTop) return cause;
              else targetList.add(cause);
            }
        }
        if (targetList.size() == 0) targetList.add(ex);
        return fromTop ? targetList.get(0) : targetList.get(targetList.size() - 1);
    }

    /**
     * remove redundant stack trace of nested cause. So less message size translating to JSON.
     * @param t
     */
    public static void cleanStackTrace(Throwable t) {
        StackTraceElement[] emptyStackTraceElements = new StackTraceElement[]{};
        int counter = 0;
        while ( counter < 20 ) {
            if ( t == null ) return;
            t.setStackTrace(emptyStackTraceElements);
            t = t.getCause();
            counter++;
        }
    }

    public static Throwable rootCause(Throwable e) {
        Throwable current = e;
        Throwable cause = e.getCause();
        while (cause != null) {
            current = cause;
            cause = current.getCause();
        }
        return current;
    }

    public static Exception rootCauseAsException(Throwable e) {
        Throwable t = rootCause(e);
        return (t instanceof Exception) ? (Exception) t : new Exception(t);
    }
}
