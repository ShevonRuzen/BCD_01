package com.bank.ejb.service;

import com.bank.core.entity.Account;
import com.bank.core.entity.Transfer;
import com.bank.core.entity.User;
import com.bank.core.exception.BusinessRuleViolationException;
import com.bank.ejb.security.SecurityAudit;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Stateless
@SecurityAudit
public class AccountSessionBean {

    private static final Logger logger = Logger.getLogger(AccountSessionBean.class.getName());

    @PersistenceContext(unitName = "BankPU")
    private EntityManager em;

    @Inject
    private NotificationService notificationService;

    private final SecureRandom random = new SecureRandom();

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Account createAccount(User owner, BigDecimal initialBalance) {
        Account account = new Account();
        account.setOwner(owner);
        account.setBalance(initialBalance != null ? initialBalance : BigDecimal.ZERO);
        account.setStatus(Account.Status.ACTIVE);
        
        // Generate unique 8-digit account number (10000000 to 99999999)
        String accountNumber;
        do {
            accountNumber = String.valueOf(10000000 + random.nextInt(90000000));
        } while (isAccountNumberExists(accountNumber));
        
        account.setAccountNumber(accountNumber);
        em.persist(account);
        
        logger.info("Created account #" + account.getId() + " with Account Number: " + accountNumber);
        return account;
    }

    private boolean isAccountNumberExists(String accNum) {
        try {
            em.createNamedQuery("Account.findByAccountNumber", Account.class)
              .setParameter("accountNumber", accNum)
              .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deposit(Long accountId, BigDecimal amount) throws BusinessRuleViolationException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Deposit amount must be positive.");
        }
        Account account = em.find(Account.class, accountId);
        if (account == null) {
            throw new BusinessRuleViolationException("Account not found.");
        }
        if (account.getStatus() != Account.Status.ACTIVE) {
            throw new BusinessRuleViolationException("Cannot deposit into an inactive account.");
        }
        
        account.setBalance(account.getBalance().add(amount));
        em.merge(account);

        // Record a Transfer log so deposit history is queryable
        Transfer depositLog = new Transfer();
        depositLog.setTransferId("DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        depositLog.setFromAccount(account);
        depositLog.setToAccount(account);
        depositLog.setAmount(amount);
        depositLog.setDescription("Self-Deposit");
        depositLog.setStatus(Transfer.Status.COMPLETED);
        depositLog.setCompletedTime(LocalDateTime.now());
        em.persist(depositLog);

        // Push notification
        notificationService.notifyUser(account.getOwner().getId(), "Deposit of $" + amount + " successful. New balance: $" + account.getBalance());
        logger.info("Deposited $" + amount + " to account number: " + account.getAccountNumber());
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Account findAccount(Long id) {
        return em.find(Account.class, id);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Account findByAccountNumber(String accountNumber) {
        try {
            return em.createNamedQuery("Account.findByAccountNumber", Account.class)
                     .setParameter("accountNumber", accountNumber)
                     .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Account> findAccountsByUser(User owner) {
        return em.createQuery("SELECT a FROM Account a WHERE a.owner = :owner", Account.class)
                 .setParameter("owner", owner)
                 .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void toggleAccountStatus(Long accountId, Account.Status status) {
        Account account = em.find(Account.class, accountId);
        if (account != null) {
            account.setStatus(status);
            em.merge(account);
            notificationService.notifyUser(account.getOwner().getId(), "Your bank account has been " + (status == Account.Status.ACTIVE ? "ACTIVATED" : "DEACTIVATED") + ".");
        }
    }
}
