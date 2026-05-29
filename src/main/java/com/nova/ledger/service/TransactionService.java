package com.nova.ledger.service;

import com.nova.ledger.dto.TransactionVO;
import com.nova.ledger.entity.Account;
import com.nova.ledger.entity.Category;
import com.nova.ledger.entity.Transaction;
import com.nova.ledger.repository.AccountRepository;
import com.nova.ledger.repository.CategoryRepository;
import com.nova.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public Page<TransactionVO> searchTransactions(Long bookId, Long userId,
                                                 LocalDateTime startDate, LocalDateTime endDate,
                                                 Long categoryId, Long accountId,
                                                 Transaction.TransactionType type, String keyword,
                                                 Pageable pageable) {
        Page<Transaction> page = transactionRepository.searchTransactions(
                bookId, startDate, endDate, categoryId, accountId, type, keyword, pageable);
        return page.map(this::toVO);
    }

    public TransactionVO getTransactionVO(Long id, Long userId) {
        return toVO(getTransaction(id, userId));
    }

    public Transaction getTransaction(Long id, Long userId) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("交易记录不存在"));
        if (!t.getUserId().equals(userId) || t.getDeleted()) {
            throw new RuntimeException("无权访问该交易记录");
        }
        return t;
    }

    private TransactionVO toVO(Transaction t) {
        String categoryName = t.getCategoryId() != null
                ? categoryRepository.findById(t.getCategoryId()).map(Category::getName).orElse(null)
                : null;
        String accountName = t.getAccountId() != null
                ? accountRepository.findById(t.getAccountId()).map(Account::getName).orElse(null)
                : null;
        String toAccountName = t.getToAccountId() != null
                ? accountRepository.findById(t.getToAccountId()).map(Account::getName).orElse(null)
                : null;
        return TransactionVO.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .bookId(t.getBookId())
                .accountId(t.getAccountId())
                .accountName(accountName)
                .toAccountId(t.getToAccountId())
                .toAccountName(toAccountName)
                .categoryId(t.getCategoryId())
                .categoryName(categoryName)
                .type(t.getType().name())
                .amount(t.getAmount())
                .transactionTime(t.getTransactionTime())
                .note(t.getNote())
                .merchant(t.getMerchant())
                .tagIds(t.getTagIds())
                .imageUrls(t.getImageUrls())
                .billId(t.getBillId())
                .build();
    }

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        Transaction saved = transactionRepository.save(transaction);
        updateAccountBalance(saved, true);
        return saved;
    }

    @Transactional
    public Transaction updateTransaction(Long id, Long userId, Transaction update) {
        Transaction original = getTransaction(id, userId);
        updateAccountBalance(original, false);
        original.setAccountId(update.getAccountId());
        original.setToAccountId(update.getToAccountId());
        original.setCategoryId(update.getCategoryId());
        original.setType(update.getType());
        original.setAmount(update.getAmount());
        original.setTransactionTime(update.getTransactionTime());
        original.setNote(update.getNote());
        original.setMerchant(update.getMerchant());
        original.setTagIds(update.getTagIds());
        Transaction saved = transactionRepository.save(original);
        updateAccountBalance(saved, true);
        return saved;
    }

    @Transactional
    public void deleteTransaction(Long id, Long userId) {
        Transaction transaction = getTransaction(id, userId);
        updateAccountBalance(transaction, false);
        transaction.setDeleted(true);
        transactionRepository.save(transaction);
    }

    private void updateAccountBalance(Transaction t, boolean isCreate) {
        BigDecimal multiplier = isCreate ? BigDecimal.ONE : BigDecimal.ONE.negate();
        switch (t.getType()) {
            case INCOME -> {
                Account account = getAccountEntity(t.getAccountId());
                account.setBalance(account.getBalance().add(t.getAmount().multiply(multiplier)));
                accountRepository.save(account);
            }
            case EXPENSE -> {
                Account account = getAccountEntity(t.getAccountId());
                account.setBalance(account.getBalance().subtract(t.getAmount().multiply(multiplier)));
                accountRepository.save(account);
            }
            case TRANSFER -> {
                if (t.getToAccountId() != null) {
                    Account from = getAccountEntity(t.getAccountId());
                    Account to = getAccountEntity(t.getToAccountId());
                    from.setBalance(from.getBalance().subtract(t.getAmount().multiply(multiplier)));
                    to.setBalance(to.getBalance().add(t.getAmount().multiply(multiplier)));
                    accountRepository.save(from);
                    accountRepository.save(to);
                }
            }
        }
    }

    private Account getAccountEntity(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("账户不存在"));
    }

    public BigDecimal sumIncome(Long bookId, LocalDateTime start, LocalDateTime end) {
        return transactionRepository.sumByTypeAndTimeRange(bookId, Transaction.TransactionType.INCOME, start, end);
    }

    public BigDecimal sumExpense(Long bookId, LocalDateTime start, LocalDateTime end) {
        return transactionRepository.sumByTypeAndTimeRange(bookId, Transaction.TransactionType.EXPENSE, start, end);
    }

    public List<Object[]> sumByCategory(Long bookId, Transaction.TransactionType type,
                                         LocalDateTime start, LocalDateTime end) {
        return transactionRepository.sumGroupByCategory(bookId, type, start, end);
    }

    public List<Object[]> trendByTimeSeries(Long bookId, Transaction.TransactionType type,
                                             LocalDateTime start, LocalDateTime end, String dateFormat) {
        return transactionRepository.sumByTimeSeries(bookId, type, start, end, dateFormat);
    }
}
