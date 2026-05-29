package com.nova.ledger.repository;

import com.nova.ledger.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByBookIdAndDeletedFalse(Long bookId);

    List<Bill> findByDeletedFalseAndAutoCreateTrueAndNextDueDateLessThanEqual(LocalDate date);
}
