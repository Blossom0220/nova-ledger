package com.nova.ledger.service;

import com.nova.ledger.entity.Account;
import com.nova.ledger.entity.Transaction;
import com.nova.ledger.repository.AccountRepository;
import com.nova.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public List<Account> getBookAccounts(Long bookId, Long userId) {
        return accountRepository.findByBookIdAndDeletedFalseOrderBySortOrderAsc(bookId);
    }

    public Account getAccount(Long id, Long userId) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("账户不存在"));
        if (!account.getUserId().equals(userId) || account.getDeleted()) {
            throw new RuntimeException("无权访问该账户");
        }
        return account;
    }

    @Transactional
    public Account createAccount(Account account) {
        account.setBalance(account.getInitialBalance());
        return accountRepository.save(account);
    }

    @Transactional
    public Account updateAccount(Long id, Long userId, Account update) {
        Account account = getAccount(id, userId);
        account.setName(update.getName());
        account.setType(update.getType());
        account.setCurrency(update.getCurrency());
        account.setIcon(update.getIcon());
        // 余额通过交易联动，不允许手动修改
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long id, Long userId) {
        Account account = getAccount(id, userId);
        account.setDeleted(true);
        accountRepository.save(account);
    }

    /**
     * 根据所有交易重新计算账户余额（用于初始化或数据修复）
     */
    @Transactional
    public void recalculateBalance(Long accountId, Long userId) {
        Account account = getAccount(accountId, userId);

        BigDecimal totalIncome = transactionRepository.sumIncomeByAccount(accountId);
        BigDecimal totalExpense = transactionRepository.sumExpenseByAccount(accountId);
        BigDecimal totalTransferIn = transactionRepository.sumTransferIn(accountId);
        BigDecimal totalTransferOut = transactionRepository.sumTransferOut(accountId, accountId);

        BigDecimal balance = account.getInitialBalance()
                .add(totalIncome)
                .add(totalTransferIn)
                .subtract(totalExpense)
                .subtract(totalTransferOut);

        account.setBalance(balance);
        accountRepository.save(account);
    }
}
