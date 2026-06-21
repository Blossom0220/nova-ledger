package com.nova.ledger.repository;

import com.nova.ledger.entity.ApiLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {

    Page<ApiLog> findByUserIdOrderByStartTimeDesc(Long userId, Pageable pageable);

    List<ApiLog> findByStartTimeBetweenOrderByStartTimeDesc(LocalDateTime start, LocalDateTime end);

    long countByStartTimeAfter(LocalDateTime after);

    long countByStartTimeAfterAndResponseStatus(LocalDateTime after, Integer status);
}
