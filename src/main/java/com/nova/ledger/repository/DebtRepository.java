package com.nova.ledger.repository;

import com.nova.ledger.entity.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    List<Debt> findByBookIdAndDeletedFalseOrderByCreatedAtDesc(Long bookId);
}
