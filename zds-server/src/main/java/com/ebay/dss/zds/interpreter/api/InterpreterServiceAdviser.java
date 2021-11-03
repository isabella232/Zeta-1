package com.ebay.dss.zds.interpreter.api;

import com.ebay.dss.zds.common.InterpreterRsp;
import com.ebay.dss.zds.exception.ApplicationBaseException;
import com.ebay.dss.zds.exception.InterpreterServiceException;
import com.ebay.dss.zds.interpreter.interpreters.jdbc.JdbcType;
import com.ebay.dss.zds.model.ZetaResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.codehaus.plexus.util.ExceptionUtils;
import org.springframework.stereotype.Component;
import sun.security.krb5.KrbException;

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.ebay.dss.zds.exception.ErrorCode.INTERPRETER_CONNECT_EXCEPTION;
import static com.ebay.dss.zds.interpreter.interpreters.jdbc.Constant.JDBC_TYPE_KEY;
import static com.ebay.dss.zds.model.ZetaStatus.*;

@Aspect
@Component
public class InterpreterServiceAdviser {

    private static final Logger interpreterServiceLogger = LogManager.getLogger(InterpreterService.class);

    @Pointcut("target(com.ebay.dss.zds.interpreter.api.InterpreterService)")
    private void inInterpreterService() {
    }

    @Pointcut("execution(public * openNote(..))")
    private void openJDBC() {
    }

    @Around("inInterpreterService() && openJDBC() && args(userName, noteId, interpreter, prop)")
    public ZetaResponse<InterpreterRsp> exceptionTranslate2ZetaResponse(
            final ProceedingJoinPoint pjp,
            final String userName,
            final String noteId,
            final String interpreter,
            final Map<String, String> prop) {
        ZetaResponse<InterpreterRsp> res;
        try {
            res = (ZetaResponse<InterpreterRsp>) pjp.proceed();
            if (res.getStatusCode().is2xxSuccessful()) {
                interpreterServiceLogger.info("Succeed to open interpreter for user: {} of note: {}", userName, noteId);
            }
            return res;
        } catch (ApplicationBaseException e) {
            throw handleException(userName, noteId, e);
        } catch (Throwable throwable) {
            interpreterServiceLogger.error("Cannot open interpreter for {}, notebook id {}, {}", userName, noteId, ExceptionUtils.getFullStackTrace(throwable));
            throw handleOtherException(userName, noteId, prop, throwable);
        }
    }

    private Throwable findKrbException(Throwable e) {
        Throwable tmp = e;
        while (Objects.nonNull(tmp) && !(tmp instanceof KrbException)) {
            tmp = tmp.getCause();
        }
        return tmp;
    }

    private Optional<Throwable> findCauseOfClass(Throwable e, Class<?> exClazz) {
        Throwable tmp = e;
        while (Objects.nonNull(tmp) && !(exClazz.isInstance(tmp))) {
            tmp = tmp.getCause();
        }
        return Optional.ofNullable(tmp);
    }

    private ApplicationBaseException handleException(String userName, String noteId, Throwable t) {
        return handleTeradataException(userName, noteId, t);
    }

    private ApplicationBaseException handleTeradataException(String userName, String noteId, Throwable t) {
        Throwable targetEx = findCauseOfClass(t, SQLException.class)
                .filter(throwable -> StringUtils.contains(throwable.getMessage(), "Teradata Database"))
                .orElse(null);
        if (Objects.nonNull(targetEx)) {
            return new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION, TERADATA_CONNECTION_EXCEPTION, noteId, targetEx.getMessage(), targetEx);
        } if (t instanceof ApplicationBaseException) {
            return (ApplicationBaseException) t;
        } else {
            return new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION, INTP_SESSION_CONNECTION_ERROR, noteId, t);
        }
    }

    private ApplicationBaseException handleOtherException(String userName, String noteId, Map<String, String> params, Throwable throwable) {

        Throwable mayKrbException = findKrbException(throwable);
        ApplicationBaseException res = null;
        if (Objects.nonNull(mayKrbException)) {
            String message = mayKrbException.getMessage();
            if (StringUtils.contains(message, "revoked (18)")) {
                res = new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION, HERMES_ACCOUNT_REVOKE_EXCEPTION, noteId, message, mayKrbException);
            } else if (StringUtils.contains(message, "invalid (24)")) {
                res = new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION, HERMES_PRE_AUTH_EXCEPTION, noteId, message, mayKrbException);
            } else if (StringUtils.contains(message, "reset (23)")) {
                res = new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION, HERMES_PWD_EXPIRED_EXCEPTION, noteId, message, mayKrbException);
            } else if (StringUtils.contains(message, "Client not found in Kerberos database (6)")) {
                res = new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION, HERMES_ACCOUNT_NOT_FOUND_EXCEPTION, noteId, message, mayKrbException);
            }
        }

        if (Objects.isNull(res)) {
            res = new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION, INTP_SESSION_CONNECTION_ERROR, noteId, mayKrbException);
        }
        return res;
    }

}
