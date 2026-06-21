package com.nova.ledger.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nova.ledger.entity.ApiLog;
import com.nova.ledger.service.ApiLogService;
import com.nova.ledger.util.NetworkUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ApiLogAspect {

    private final ApiLogService apiLogService;
    private final ObjectMapper objectMapper;

    @Pointcut("execution(* com.nova.ledger.controller..*.*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();

        ApiLog.ApiLogBuilder logBuilder = ApiLog.builder()
                .requestUrl(request.getRequestURI())
                .requestMethod(request.getMethod())
                .requestParams(request.getQueryString())
                .ip(NetworkUtil.getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .startTime(startTime);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            logBuilder.userId(userId);
        }

        String[] paramNames = {};
        try {
            paramNames = org.aspectj.lang.reflect.MethodSignature.class.cast(joinPoint.getSignature())
                    .getParameterNames();
        } catch (Exception ignored) {}

        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                        || arg instanceof Authentication) {
                    continue;
                }
                if (paramNames != null && i < paramNames.length) {
                    sb.append(paramNames[i]).append("=");
                }
                try {
                    sb.append(objectMapper.writeValueAsString(arg));
                } catch (Exception e) {
                    sb.append(arg);
                }
                sb.append(", ");
            }
            String reqBody = sb.toString();
            if (reqBody.endsWith(", ")) {
                reqBody = reqBody.substring(0, reqBody.length() - 2);
            }
            if (reqBody.length() > 4000) {
                reqBody = reqBody.substring(0, 4000);
            }
            logBuilder.requestBody(reqBody);
        }

        Object result;
        try {
            result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startMs;

            int statusCode = (response != null) ? response.getStatus() : 200;
            logBuilder.responseStatus(statusCode)
                    .endTime(LocalDateTime.now())
                    .executionTime(duration);

            if (result != null) {
                try {
                    String respStr = objectMapper.writeValueAsString(result);
                    if (respStr.length() > 4000) {
                        respStr = respStr.substring(0, 4000);
                    }
                    logBuilder.responseBody(respStr);
                } catch (Exception ignored) {}
            }

            log.debug("[API-LOG] {} {} - {}ms - {}", request.getMethod(), request.getRequestURI(), duration, statusCode);
            saveLog(logBuilder.build());
            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startMs;
            int statusCode = (response != null) ? response.getStatus() : 500;
            logBuilder.responseStatus(statusCode)
                    .endTime(LocalDateTime.now())
                    .executionTime(duration)
                    .exceptionMessage(ex.getMessage() != null ? ex.getMessage().substring(0, Math.min(ex.getMessage().length(), 2000)) : null);

            log.warn("[API-LOG] {} {} - ERROR({}ms): {}", request.getMethod(), request.getRequestURI(), duration, ex.getMessage());
            saveLog(logBuilder.build());
            throw ex;
        }
    }

    private void saveLog(ApiLog apiLog) {
        apiLogService.saveLog(apiLog);
    }
}
