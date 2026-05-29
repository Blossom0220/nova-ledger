package com.nova.ledger.service;

import com.nova.ledger.entity.Debt;
import com.nova.ledger.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;

    public List<Debt> getDebts(Long bookId, Long userId) {
        return debtRepository.findByBookIdAndDeletedFalseOrderByCreatedAtDesc(bookId);
    }

    @Transactional
    public Debt createDebt(Debt debt) {
        return debtRepository.save(debt);
    }

    @Transactional
    public Debt updateDebt(Long id, Long userId, Debt update) {
        Debt debt = getDebt(id, userId);
        debt.setCounterparty(update.getCounterparty());
        debt.setAmount(update.getAmount());
        debt.setRepaidAmount(update.getRepaidAmount());
        debt.setNote(update.getNote());
        debt.setDueDate(update.getDueDate());
        debt.setStatus(update.getStatus());

        // 如果已还清则自动设为 SETTLED
        if (debt.getRepaidAmount().compareTo(debt.getAmount()) >= 0) {
            debt.setStatus(Debt.DebtStatus.SETTLED);
        }

        return debtRepository.save(debt);
    }

    @Transactional
    public void deleteDebt(Long id, Long userId) {
        Debt debt = getDebt(id, userId);
        debt.setDeleted(true);
        debtRepository.save(debt);
    }

    public Debt getDebt(Long id, Long userId) {
        Debt debt = debtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("债权/债务不存在"));
        if (!debt.getUserId().equals(userId) || debt.getDeleted()) {
            throw new RuntimeException("无权访问");
        }
        return debt;
    }
}
