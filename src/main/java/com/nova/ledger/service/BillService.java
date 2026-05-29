package com.nova.ledger.service;

import com.nova.ledger.entity.Bill;
import com.nova.ledger.entity.Transaction;
import com.nova.ledger.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final TransactionService transactionService;

    public List<Bill> getBills(Long bookId, Long userId) {
        return billRepository.findByBookIdAndDeletedFalse(bookId);
    }

    @Transactional
    public Bill createBill(Bill bill) {
        return billRepository.save(bill);
    }

    @Transactional
    public Bill updateBill(Long id, Long userId, Bill update) {
        Bill bill = getBill(id, userId);
        bill.setName(update.getName());
        bill.setAmount(update.getAmount());
        bill.setFrequency(update.getFrequency());
        bill.setCustomCron(update.getCustomCron());
        bill.setNextDueDate(update.getNextDueDate());
        bill.setAutoCreate(update.getAutoCreate());
        bill.setCategoryId(update.getCategoryId());
        bill.setAccountId(update.getAccountId());
        return billRepository.save(bill);
    }

    @Transactional
    public void deleteBill(Long id, Long userId) {
        Bill bill = getBill(id, userId);
        bill.setDeleted(true);
        billRepository.save(bill);
    }

    public Bill getBill(Long id, Long userId) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("周期账单不存在"));
        if (!bill.getUserId().equals(userId) || bill.getDeleted()) {
            throw new RuntimeException("无权访问该周期账单");
        }
        return bill;
    }

    /**
     * 定时任务：每日凌晨检查到期账单并自动生成交易
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void autoCreateBillTransactions() {
        List<Bill> dueBills = billRepository
                .findByDeletedFalseAndAutoCreateTrueAndNextDueDateLessThanEqual(LocalDate.now());

        for (Bill bill : dueBills) {
            try {
                Transaction transaction = Transaction.builder()
                        .userId(bill.getUserId())
                        .bookId(bill.getBookId())
                        .accountId(bill.getAccountId())
                        .categoryId(bill.getCategoryId())
                        .type(Transaction.TransactionType.EXPENSE)
                        .amount(bill.getAmount())
                        .transactionTime(LocalDateTime.now())
                        .note(bill.getName() + " (自动记账)")
                        .billId(bill.getId())
                        .build();

                transactionService.createTransaction(transaction);

                // 计算下一期到期日
                LocalDate nextDue = switch (bill.getFrequency()) {
                    case DAILY -> bill.getNextDueDate().plusDays(1);
                    case WEEKLY -> bill.getNextDueDate().plusWeeks(1);
                    case MONTHLY -> bill.getNextDueDate().plusMonths(1);
                    case YEARLY -> bill.getNextDueDate().plusYears(1);
                    case CUSTOM -> bill.getNextDueDate(); // 自定义暂不处理
                };

                bill.setNextDueDate(nextDue);
                billRepository.save(bill);

                log.info("Auto-created transaction for bill: {} (user: {})", bill.getName(), bill.getUserId());
            } catch (Exception e) {
                log.error("Failed to auto-create transaction for bill: {}", bill.getName(), e);
            }
        }
    }
}
