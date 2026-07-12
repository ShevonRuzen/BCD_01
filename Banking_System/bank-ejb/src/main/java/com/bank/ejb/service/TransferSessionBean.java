package com.bank.ejb.service;

import com.bank.core.entity.Account;
import com.bank.core.entity.Transfer;
import com.bank.core.entity.User;
import com.bank.core.exception.BusinessRuleViolationException;
import com.bank.ejb.security.SecurityAudit;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Stateless
@SecurityAudit
public class TransferSessionBean {

    private static final Logger logger = Logger.getLogger(TransferSessionBean.class.getName());

    /** 2.45% fee deducted from sender's account (separate from the principal) */
    private static final BigDecimal FEE_RATE = new BigDecimal("0.0245");

    @PersistenceContext(unitName = "BankPU")
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    @Resource
    private TimerService timerService;

    @Inject
    private NotificationService notificationService;

    // -----------------------------------------------------------------------
    // Helper: find the first ACTIVE admin bank account (managed entity)
    // -----------------------------------------------------------------------
    private Account findAdminAccount() {
        List<Account> list = em.createQuery(
            "SELECT a FROM Account a WHERE a.owner.userType = :type AND a.status = :status",
            Account.class)
            .setParameter("type",   User.UserType.ADMIN)
            .setParameter("status", Account.Status.ACTIVE)
            .setMaxResults(1)
            .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    // -----------------------------------------------------------------------
    // Helper: record a fee transfer ledger entry
    // -----------------------------------------------------------------------
    private void recordFeeTransfer(Account from, Account adminAcc, BigDecimal fee, String parentTxnId) {
        Transfer ft = new Transfer();
        ft.setTransferId("FEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ft.setFromAccount(from);
        ft.setToAccount(adminAcc);
        ft.setAmount(fee);
        ft.setDescription("Transaction Fee 2.45% for " + parentTxnId);
        ft.setStatus(Transfer.Status.COMPLETED);
        ft.setCompletedTime(LocalDateTime.now());
        em.persist(ft);
    }

    // -----------------------------------------------------------------------
    // INSTANT TRANSFER — principal: from → to; fee: from → admin (separately)
    // -----------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transfer executeInstantTransfer(String fromAccountNumber, String toAccountNumber,
                                           BigDecimal amount, String description)
            throws BusinessRuleViolationException {

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessRuleViolationException("Transfer amount must be positive");

        Account fromAccount = findByNumber(fromAccountNumber);
        Account toAccount   = findByNumber(toAccountNumber);

        if (fromAccount.getId().equals(toAccount.getId()))
            throw new BusinessRuleViolationException("Cannot transfer to same account");

        BigDecimal fee        = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);

        if (fromAccount.getBalance().compareTo(totalDebit) < 0)
            throw new BusinessRuleViolationException(
                "Insufficient funds. Need $" + totalDebit +
                " (principal $" + amount + " + 2.45% fee $" + fee + ").");

        // Persist the transfer record first
        Transfer transfer = new Transfer();
        transfer.setTransferId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transfer.setFromAccount(fromAccount);
        transfer.setToAccount(toAccount);
        transfer.setAmount(amount);
        transfer.setDescription(description != null && !description.isBlank() ? description : "Direct Transfer");
        transfer.setStatus(Transfer.Status.IN_PROGRESS);
        em.persist(transfer);

        try {
            // Move principal
            fromAccount.setBalance(fromAccount.getBalance().subtract(totalDebit)); // deduct principal + fee
            toAccount.setBalance(toAccount.getBalance().add(amount));              // recipient gets exact amount
            em.merge(fromAccount);
            em.merge(toAccount);

            transfer.setStatus(Transfer.Status.COMPLETED);
            transfer.setCompletedTime(LocalDateTime.now());
            em.merge(transfer);

            // Route fee to admin
            Account adminAccount = findAdminAccount();
            if (adminAccount != null) {
                adminAccount.setBalance(adminAccount.getBalance().add(fee));
                em.merge(adminAccount);
                recordFeeTransfer(fromAccount, adminAccount, fee, transfer.getTransferId());
                logger.info("Fee $" + fee + " routed to admin account " + adminAccount.getAccountNumber());
            } else {
                logger.warning("No active admin account found — fee $" + fee + " not routed.");
            }

            notificationService.notifyUser(fromAccount.getOwner().getId(),
                "Transfer of $" + amount + " to " + toAccount.getOwner().getName()
                + " completed. Fee charged: $" + fee + ".");
            notificationService.notifyUser(toAccount.getOwner().getId(),
                "Received $" + amount + " from " + fromAccount.getOwner().getName() + ".");

            return transfer;

        } catch (Exception e) {
            sessionContext.setRollbackOnly();
            logger.severe("Instant transfer failed: " + e.getMessage());
            throw new RuntimeException("System error during transfer", e);
        }
    }

    // -----------------------------------------------------------------------
    // SCHEDULE TRANSFER — stores a SCHEDULED record + EJB timer
    // -----------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Transfer scheduleTransfer(String fromAccountNumber, String toAccountNumber,
                                     BigDecimal amount, String description,
                                     LocalDateTime scheduledTime)
            throws BusinessRuleViolationException {

        if (scheduledTime.isBefore(LocalDateTime.now()))
            throw new BusinessRuleViolationException("Scheduled time must be in the future");

        Account fromAccount = findByNumber(fromAccountNumber);
        Account toAccount   = findByNumber(toAccountNumber);

        // Pre-validate balance (fee will be charged at execution)
        BigDecimal fee        = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);
        if (fromAccount.getBalance().compareTo(totalDebit) < 0)
            throw new BusinessRuleViolationException(
                "Insufficient funds to schedule. Need $" + totalDebit +
                " (principal + 2.45% fee $" + fee + ").");

        Transfer transfer = new Transfer();
        transfer.setTransferId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transfer.setFromAccount(fromAccount);
        transfer.setToAccount(toAccount);
        transfer.setAmount(amount);
        transfer.setDescription(description != null && !description.isBlank() ? description : "Scheduled Transfer");
        transfer.setStatus(Transfer.Status.SCHEDULED);
        transfer.setScheduledTime(scheduledTime);
        em.persist(transfer);

        Date execDate = Date.from(scheduledTime.atZone(ZoneId.systemDefault()).toInstant());
        TimerConfig cfg = new TimerConfig("ScheduledTransfer:" + transfer.getId(), true);
        timerService.createSingleActionTimer(execDate, cfg);

        logger.info("Scheduled transfer " + transfer.getTransferId() + " for " + scheduledTime);
        return transfer;
    }

    // -----------------------------------------------------------------------
    // TIMER CALLBACK — executes the scheduled transfer WITH 2.45% fee
    // NOTE: REQUIRES_NEW ensures a fresh EntityManager — all entities MUST
    //       be re-fetched with em.find() to avoid detached proxy issues.
    // -----------------------------------------------------------------------
    @Timeout
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void processScheduledTimer(Timer timer) {
        String info = (String) timer.getInfo();
        if (info == null || !info.startsWith("ScheduledTransfer:")) return;

        Long transferId = Long.parseLong(info.split(":")[1]);
        // Re-fetch the Transfer within this NEW transaction
        Transfer transfer = em.find(Transfer.class, transferId);
        if (transfer == null || transfer.getStatus() != Transfer.Status.SCHEDULED) return;

        // Re-fetch accounts within this NEW transaction (avoid lazy-proxy detach bug)
        Account fromAccount = em.find(Account.class, transfer.getFromAccount().getId());
        Account toAccount   = em.find(Account.class, transfer.getToAccount().getId());

        BigDecimal amount     = transfer.getAmount();
        BigDecimal fee        = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);

        try {
            transfer.setStatus(Transfer.Status.IN_PROGRESS);
            em.merge(transfer);

            if (fromAccount.getBalance().compareTo(totalDebit) < 0) {
                transfer.setStatus(Transfer.Status.FAILED);
                transfer.setDescription(transfer.getDescription() + " [FAILED: insufficient funds for fee]");
                em.merge(transfer);
                notificationService.notifyUser(fromAccount.getOwner().getId(),
                    "Scheduled transfer of $" + amount + " FAILED: need $" + totalDebit
                    + " (incl. 2.45% fee $" + fee + ").");
                return;
            }

            // Debit sender: principal + fee
            fromAccount.setBalance(fromAccount.getBalance().subtract(totalDebit));
            // Credit recipient: exact principal only
            toAccount.setBalance(toAccount.getBalance().add(amount));
            em.merge(fromAccount);
            em.merge(toAccount);

            transfer.setStatus(Transfer.Status.COMPLETED);
            transfer.setCompletedTime(LocalDateTime.now());
            em.merge(transfer);

            // Route fee to admin — re-fetch admin account fresh within this transaction
            Account adminAccount = findAdminAccount();
            if (adminAccount != null) {
                adminAccount.setBalance(adminAccount.getBalance().add(fee));
                em.merge(adminAccount);
                recordFeeTransfer(fromAccount, adminAccount, fee, transfer.getTransferId());
                logger.info("Scheduled transfer fee $" + fee + " routed to admin acc "
                    + adminAccount.getAccountNumber());
            } else {
                logger.warning("No active admin account found for fee routing on scheduled transfer "
                    + transfer.getTransferId());
            }

            notificationService.notifyUser(fromAccount.getOwner().getId(),
                "Scheduled transfer of $" + amount + " to " + toAccount.getOwner().getName()
                + " executed. Fee: $" + fee + ".");
            notificationService.notifyUser(toAccount.getOwner().getId(),
                "Received scheduled transfer of $" + amount + " from "
                + fromAccount.getOwner().getName() + ".");

        } catch (Exception e) {
            transfer.setStatus(Transfer.Status.FAILED);
            em.merge(transfer);
            logger.severe("Scheduled timer execution failed for transfer " + transferId + ": " + e.getMessage());
            notificationService.notifyUser(fromAccount.getOwner().getId(),
                "Scheduled transfer of $" + amount + " failed: system error.");
        }
    }

    // -----------------------------------------------------------------------
    // CANCEL SCHEDULED TRANSFER
    // Either the SENDER or the RECIPIENT can cancel a scheduled transfer.
    // -----------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void cancelScheduledTransfer(Long transferId, String userEmail)
            throws BusinessRuleViolationException {

        Transfer transfer = em.find(Transfer.class, transferId);
        if (transfer == null)
            throw new BusinessRuleViolationException("Transfer record not found.");

        String senderEmail    = transfer.getFromAccount().getOwner().getEmail();
        String recipientEmail = transfer.getToAccount().getOwner().getEmail();

        boolean isSender    = senderEmail.equals(userEmail);
        boolean isRecipient = recipientEmail.equals(userEmail);

        if (!isSender && !isRecipient)
            throw new SecurityException("Unauthorized: You are not a party to this transfer.");

        if (transfer.getStatus() != Transfer.Status.SCHEDULED)
            throw new BusinessRuleViolationException("Only SCHEDULED transfers can be cancelled.");

        // Cancel the EJB programmatic timer
        for (Timer t : timerService.getTimers()) {
            if (t.getInfo() != null && t.getInfo().equals("ScheduledTransfer:" + transfer.getId())) {
                t.cancel();
                logger.info("EJB timer cancelled for transfer " + transferId);
                break;
            }
        }

        transfer.setStatus(Transfer.Status.CANCELLED);
        em.merge(transfer);

        // Notify both parties
        notificationService.notifyUser(transfer.getFromAccount().getOwner().getId(),
            "Scheduled transfer " + transfer.getTransferId() + " was cancelled.");
        if (!senderEmail.equals(recipientEmail)) {
            notificationService.notifyUser(transfer.getToAccount().getOwner().getId(),
                "A scheduled incoming transfer " + transfer.getTransferId() + " was cancelled.");
        }
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Transfer> getRecentTransfers(String accountNumber) {
        return em.createQuery(
            "SELECT t FROM Transfer t WHERE t.fromAccount.accountNumber = :num " +
            "OR t.toAccount.accountNumber = :num ORDER BY t.id DESC",
            Transfer.class)
            .setParameter("num", accountNumber)
            .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Transfer> getDepositHistory(String accountNumber) {
        return em.createQuery(
            "SELECT t FROM Transfer t WHERE t.fromAccount.accountNumber = :num " +
            "AND t.toAccount.accountNumber = :num AND t.description = 'Self-Deposit' ORDER BY t.id DESC",
            Transfer.class)
            .setParameter("num", accountNumber)
            .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Transfer> getScheduledTransfers(String accountNumber) {
        return em.createQuery(
            "SELECT t FROM Transfer t WHERE t.fromAccount.accountNumber = :num " +
            "AND t.status = :status ORDER BY t.scheduledTime ASC",
            Transfer.class)
            .setParameter("num", accountNumber)
            .setParameter("status", Transfer.Status.SCHEDULED)
            .getResultList();
    }

    // -----------------------------------------------------------------------
    // Weekly maintenance cleanup
    // -----------------------------------------------------------------------
    @jakarta.ejb.Schedule(dayOfWeek = "Sun", hour = "0", minute = "0", second = "0",
                          persistent = true, info = "WeeklyMaintenance")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void weeklyCleanup() {
        logger.info("Starting Weekly Maintenance Cleanup Job");
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        try {
            int deleted = em.createQuery(
                "DELETE FROM Transfer t WHERE t.status = :status AND t.createdAt < :threshold")
                .setParameter("status", Transfer.Status.FAILED)
                .setParameter("threshold", threshold)
                .executeUpdate();
            logger.info("Weekly cleanup complete. Removed " + deleted + " failed transfers.");
        } catch (Exception e) {
            logger.severe("Weekly cleanup failed: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------
    private Account findByNumber(String number) throws BusinessRuleViolationException {
        List<Account> list = em.createQuery(
            "SELECT a FROM Account a WHERE a.accountNumber = :num", Account.class)
            .setParameter("num", number)
            .getResultList();
        if (list.isEmpty())
            throw new BusinessRuleViolationException("Account not found: " + number);
        return list.get(0);
    }
}
