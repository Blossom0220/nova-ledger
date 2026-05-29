package com.nova.ledger.repository;

import com.nova.ledger.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByBookIdAndDeletedFalseOrderBySortOrderAsc(Long bookId);

    List<Account> findByUserIdAndDeletedFalse(Long userId);
}
