package com.ebay.dss.zds.interpreter;

import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class InterpreterManagerExecutor extends ThreadPoolExecutor {

    public static final int CORE_POOL_SIZE = 5;
    public static final int MAXIMUM_POOL_SIZE = 100;
    public static final long KEEP_ALIVE_TIME = 3L;
    public static final TimeUnit TIME_UNIT = TimeUnit.MINUTES;

    public InterpreterManagerExecutor() {
        super(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TIME_UNIT, new LinkedBlockingQueue<>());
    }
}
