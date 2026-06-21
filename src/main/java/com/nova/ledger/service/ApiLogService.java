package com.nova.ledger.service;

import com.nova.ledger.entity.ApiLog;
import com.nova.ledger.repository.ApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiLogService {

    private final ApiLogRepository apiLogRepository;

    @Async("logAsyncExecutor")
    public void saveLog(ApiLog apiLog) {
        try {
            apiLogRepository.save(apiLog);
        } catch (Exception e) {
            log.error("Failed to save API log: {}", e.getMessage());
        }
    }
}
