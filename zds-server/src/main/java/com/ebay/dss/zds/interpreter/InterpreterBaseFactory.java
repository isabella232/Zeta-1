package com.ebay.dss.zds.interpreter;

import com.ebay.dss.zds.common.Metric2Report;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

import static com.ebay.dss.zds.common.Metric2Report.Utils.setSomeTagsFromInterpreter;
import static com.ebay.dss.zds.common.Metric2Report.Utils.setYearMonthTags;

@Component
public class InterpreterBaseFactory {

    public InterpreterBaseFactory() {
    }

    public Interpreter create(String className, Properties prop) throws Exception {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setSuperclass(Class.forName(className));
        proxyFactory.setFilter(TrackInterpreterMethodHandler.METHOD_FILTER);
        return (Interpreter) proxyFactory.create(new Class[]{Properties.class}, new Object[]{prop},
                new TrackInterpreterMethodHandler(className));
    }

    private static class TrackInterpreterMethodHandler implements MethodHandler {

        private static final String OPEN_METHOD = "open";
        private static final String GET_CLASS_NAME_METHOD = "getClassName";
        private static final Collection<String> PROXY_METHODS = Arrays.asList(OPEN_METHOD, GET_CLASS_NAME_METHOD);
        private static final MethodFilter METHOD_FILTER = m -> PROXY_METHODS.contains(m.getName());
        private String innerClassName;

        TrackInterpreterMethodHandler(String innerClassName) {
            this.innerClassName = innerClassName;
        }

        @Override
        public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
            if (OPEN_METHOD.equals(thisMethod.getName())) {
                return open(self, proceed, args);
            } else if (GET_CLASS_NAME_METHOD.equals(thisMethod.getName())) {
                return getClassName();
            }
            throw new IllegalArgumentException("This interpreter proxy can not handle current method: " + proceed.getName());
        }

        private Object open(Object self, Method method, Object[] args) throws Throwable {
            Interpreter interpreter = (Interpreter) self;
            Map<String, String> tags = new HashMap<>();
            setSomeTagsFromInterpreter(interpreter, tags);
            setYearMonthTags(tags);
            Instant start = Instant.now();
            try {
                Object retVal = method.invoke(interpreter, args);
                tags.put("status", "success");
                return retVal;
            } catch (InvocationTargetException e) {
                tags.put("status", "failure");
                throw e.getTargetException();
            } finally {
                Instant end = Instant.now();
                long duration = end.toEpochMilli() - start.toEpochMilli();
            }
        }

        private Object getClassName() throws Throwable {
            return innerClassName;
        }
    }

}
