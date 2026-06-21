package com.nova.ledger.config;

import com.nova.ledger.util.NetworkUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLogFilter implements Filter {

    private static final String[] IGNORE_URIS = {"/actuator", "/favicon.ico"};

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String uri = request.getRequestURI();
        for (String ignore : IGNORE_URIS) {
            if (uri.startsWith(ignore)) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = Instant.now().toEpochMilli();

        log.info("<<<<<< Request Start >>>>>>");
        logRequest(requestWrapper);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = Instant.now().toEpochMilli() - startTime;
            logResponse(responseWrapper, duration);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String remoteAddr = NetworkUtil.getClientIp(request);

        log.info("[{}] {} {}", method, uri,
                queryString != null ? "?" + queryString : "");
        log.info("  IP           : {}", remoteAddr);
        log.info("  UserAgent    : {}", request.getHeader("User-Agent"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            log.info("  UserId       : {}", userId);
        }

        if (log.isDebugEnabled()) {
            Map<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if (!"Authorization".equalsIgnoreCase(name)) {
                    headers.put(name, request.getHeader(name));
                }
            }
            log.debug("  Headers      : {}", headers);

            byte[] body = ((ContentCachingRequestWrapper) request).getContentAsByteArray();
            if (body.length > 0) {
                String bodyStr = new String(body, StandardCharsets.UTF_8);
                if (bodyStr.length() > 2000) {
                    bodyStr = bodyStr.substring(0, 2000) + "...(truncated)";
                }
                log.debug("  RequestBody  : {}", bodyStr);
            }
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();

        if (status >= 400) {
            log.warn("[RESPONSE] Status={}, Duration={}ms", status, duration);
        } else {
            log.info("[RESPONSE] Status={}, Duration={}ms", status, duration);
        }

        if (log.isDebugEnabled()) {
            byte[] body = response.getContentAsByteArray();
            if (body.length > 0) {
                String bodyStr = new String(body, StandardCharsets.UTF_8);
                if (bodyStr.length() > 500) {
                    bodyStr = bodyStr.substring(0, 500) + "...(truncated)";
                }
                log.debug("  ResponseBody : {}", bodyStr);
            }
        }
        log.info("<<<<<< Request End >>>>>>");
    }
}
