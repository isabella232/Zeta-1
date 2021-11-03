package com.ebay.dss.zds.rest.filter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

import static com.ebay.dss.zds.common.Constant.ZDS_SERVER_REQUEST_ID_ATTR;

@Component
public class RequestIdGenerateFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestIdGenerateFilter.class);

    public static UUID checkAndGetRequestId(String maybeRequestId) {
        UUID requestId = null;
        try {
            if (StringUtils.isNotBlank(maybeRequestId)) {
                requestId = UUID.fromString(maybeRequestId);
                if (log.isTraceEnabled()) {
                    log.trace("Found request id {} exist in response headers.", requestId);
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Get request id error", e);
            }
        }
        return requestId;
    }

    private static UUID getRequestIdIfInResponse(HttpServletResponse response) {
        String maybeRequestId = response.getHeader(ZDS_SERVER_REQUEST_ID_ATTR);
        return checkAndGetRequestId(maybeRequestId);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UUID requestId = getRequestIdIfInResponse(response);
        boolean notFound = requestId == null;
        if (notFound) {
            requestId = UUID.randomUUID();
            if (log.isTraceEnabled()) {
                log.trace("Generate request id {}", requestId);
            }
            response.setHeader(ZDS_SERVER_REQUEST_ID_ATTR, requestId.toString());
        }
        request.getSession().setAttribute(ZDS_SERVER_REQUEST_ID_ATTR, requestId);
        filterChain.doFilter(request, response);
    }

}
