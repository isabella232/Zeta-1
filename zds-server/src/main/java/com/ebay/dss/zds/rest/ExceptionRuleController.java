package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.exception.EntityNotFoundException;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.model.ExceptionRule;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.rest.error.match.ExceptionRuleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/ex_rule")
public class ExceptionRuleController {

    private final ExceptionRuleRepository ruleRepository;

    public ExceptionRuleController(ExceptionRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @GetMapping
    public ResponseEntity<Page<ExceptionRule>> listRules(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "id") String sortField) {
        Page<ExceptionRule> rules = ruleRepository.findAll(PageRequest.of(page, size, Sort.by(sortField)));
        return ZetaResponse.success(rules);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExceptionRule> getRule(@PathVariable int id) {
        return ZetaResponse.success(ruleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("not find rule " + id)));
    }

    @PostMapping
    public ResponseEntity<ExceptionRule> createRule(@RequestBody ExceptionRule rule) {
        ExceptionRule newRule = adjustErrorCode(rule);
        return ZetaResponse.success(ruleRepository.save(newRule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExceptionRule> updateRule(
            @PathVariable int id,
            @RequestBody  ExceptionRule rule) {
        ExceptionRule newRule = adjustErrorCode(rule);
        newRule.setId(id);
        return ZetaResponse.success(ruleRepository.save(newRule));
    }

    private ExceptionRule adjustErrorCode(ExceptionRule rule) {
        if (!rule.isMessageOnly() && ErrorCode.fromUnsafe(rule.getErrorCode()) == null) {
            throw new EntityNotFoundException("error code not found " + rule.getErrorCode());
        }
        rule.setErrorCode(ErrorCode.from(rule.getErrorCode()).name());
        return rule;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable int id) {
        ruleRepository.deleteById(id);
    }

    @PostMapping("/test_error")
    public void test(@RequestBody TestError testObj) throws Throwable {
        throw (Throwable) testObj.getElement();
    }

    public interface TestErrorElement {
        Class<?> getElementClass() throws ClassNotFoundException;

        Object getElement() throws Exception;
    }

    public static class TestError implements TestErrorElement {
        private String className;
        private List<TestErrorArg> args;

        public String getClassName() {
            return className;
        }

        public TestError setClassName(String className) {
            this.className = className;
            return this;
        }

        public List<TestErrorArg> getArgs() {
            return args;
        }

        public TestError setArgs(List<TestErrorArg> args) {
            this.args = args;
            return this;
        }

        public Class<?> getElementClass() throws ClassNotFoundException {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        }

        private Class<?>[] getArgsClasses() throws ClassNotFoundException {
            List<Class<?>> argsClasses = new ArrayList<>();
            for (TestErrorArg o : args) {
                argsClasses.add(o.getElementClass());
            }
            return argsClasses.toArray(new Class[0]);
        }

        private Constructor<?> getObjectConstructor() throws ClassNotFoundException, NoSuchMethodException {
            return getElementClass()
                    .getConstructor(getArgsClasses());
        }

        private Method getFactoryMethod(String methodName) throws ClassNotFoundException, NoSuchMethodException {
            return getElementClass()
                    .getDeclaredMethod(methodName, getArgsClasses());
        }

        public Object getElement() throws Exception {
            List<Object> args = new ArrayList<>();
            for (TestErrorArg o : this.args) {
                args.add(o.getElement());
            }
            if (getElementClass().isEnum()) {
                return getFactoryMethod("valueOf").invoke(null, args.toArray());
            }
            return getObjectConstructor()
                    .newInstance(args.toArray());
        }
    }


    public static class TestErrorArg implements TestErrorElement {
        private Object simpleArg;
        private TestError arg;

        public Object getSimpleArg() {
            return simpleArg;
        }

        public TestErrorArg setSimpleArg(Object simpleArg) {
            this.simpleArg = simpleArg;
            return this;
        }

        public TestError getArg() {
            return arg;
        }

        public TestErrorArg setArg(TestError arg) {
            this.arg = arg;
            return this;
        }

        @Override
        public Class<?> getElementClass() throws ClassNotFoundException {
            return simpleArg != null ? simpleArg.getClass() : arg.getElementClass();
        }

        @Override
        public Object getElement() throws Exception {
            return simpleArg != null ? simpleArg : arg.getElement();
        }
    }
}
