package com.nova.ledger.repository;

import com.nova.ledger.entity.UserLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserLoginLogRepository extends JpaRepository<UserLoginLog, Long> {

    List<UserLoginLog> findByUserIdAndLoginTimeAfterOrderByLoginTimeDesc(Long userId, LocalDateTime after);

    long countByUserIdAndSuccessAndLoginTimeAfter(Long userId, boolean success, LocalDateTime after);
}
