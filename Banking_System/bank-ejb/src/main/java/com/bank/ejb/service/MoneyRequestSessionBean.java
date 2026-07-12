package com.bank.ejb.service;

import com.bank.core.entity.Account;
import com.bank.core.entity.MoneyRequest;
import com.bank.core.entity.Transfer;
import com.bank.core.entity.User;
import com.bank.core.exception.BusinessRuleViolationException;
import com.bank.ejb.security.SecurityAudit;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Stateless
@SecurityAudit
public class MoneyRequestSessionBean {

    private static final Logger logger = Logger.getLogger(MoneyRequestSessionBean.class.getName());

    /** 2.45% fee charged to payer's account when they approve a money request */
    private static final BigDecimal FEE_RATE = new BigDecimal("0.0245");

    @PersistenceContext(unitName = "BankPU")
    private EntityManager em;

    @Inject
    private UserSessionBean userService;

    @Inject
    private NotificationService notificationService;

    // -------------------------------------------------------------------------
    // Helper: first active admin account
    // -------------------------------------------------------------------------
    private Account findAdminAccount() {
        List<Account> admins = em.createQuery(
            "SELECT a FROM Account a WHERE a.owner.userType = :type AND a.status = :status",
            Account.class)
            .setParameter("type", User.UserType.ADMIN)
            .setParameter("status", Account.Status.ACTIVE)
            .setMaxResults(1)
            .getResultList();
        return admins.isEmpty() ? null : admins.get(0);
    }

    // -------------------------------------------------------------------------
    // Create a money request (requester asks payer to send them money)
    // -------------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public MoneyRequest createRequest(String requesterEmail, String payerAccountNumber,
                                      BigDecimal amount, String description)
            throws BusinessRuleViolationException {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Request amount must be positive.");
        }

        User requesterUser = userService.findByEmail(requesterEmail);
        List<Account> reqAccounts = em.createQuery(
            "SELECT a FROM Account a WHERE a.owner = :owner", Account.class)
            .setParameter("owner", requesterUser)
            .getResultList();
        if (reqAccounts.isEmpty()) {
            throw new BusinessRuleViolationException("You do not have a bank account to receive funds.");
        }
        Account requesterAccount = reqAccounts.get(0);

        List<Account> payerAccounts = em.createQuery(
            "SELECT a FROM Account a WHERE a.accountNumber = :num", Account.class)
            .setParameter("num", payerAccountNumber)
            .getResultList();
        if (payerAccounts.isEmpty()) {
            throw new BusinessRuleViolationException("Payer account number not found.");
        }
        Account payerAccount = payerAccounts.get(0);

        if (requesterAccount.getId().equals(payerAccount.getId())) {
            throw new BusinessRuleViolationException("You cannot request money from yourself.");
        }

        MoneyRequest request = new MoneyRequest();
        request.setRequestId("REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        request.setRequester(requesterAccount);
        request.setPayer(payerAccount);
        request.setAmount(amount);
        request.setDescription(description);
        request.setStatus(MoneyRequest.Status.PENDING);
        em.persist(request);

        notificationService.notifyUser(payerAccount.getOwner().getId(),
            requesterUser.getName() + " is requesting $" + amount + " from you.");

        return request;
    }

    // -------------------------------------------------------------------------
    // Approve request — payer sends principal to requester; 2.45% fee goes to admin
    //   - fee is deducted from payer's account SEPARATELY (not from the principal)
    // -------------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void approveRequest(Long requestId, String payerEmail) throws BusinessRuleViolationException {
        MoneyRequest request = em.find(MoneyRequest.class, requestId);
        if (request == null) throw new BusinessRuleViolationException("Money request not found.");
        if (request.getStatus() != MoneyRequest.Status.PENDING)
            throw new BusinessRuleViolationException("This request has already been processed.");

        Account payerAccount = request.getPayer();
        if (!payerAccount.getOwner().getEmail().equals(payerEmail))
            throw new SecurityException("Unauthorized: You do not own the account requested to pay.");

        BigDecimal amount     = request.getAmount();
        BigDecimal fee        = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDebit = amount.add(fee);   // principal + fee both from payer

        if (payerAccount.getBalance().compareTo(totalDebit) < 0) {
            throw new BusinessRuleViolationException(
                "Insufficient funds. You need $" + totalDebit +
                " ($" + amount + " principal + $" + fee + " fee 2.45%).");
        }

        Account requesterAccount = request.getRequester();

        // Atomic ledger updates
        payerAccount.setBalance(payerAccount.getBalance().subtract(totalDebit));
        requesterAccount.setBalance(requesterAccount.getBalance().add(amount));

        em.merge(payerAccount);
        em.merge(requesterAccount);

        // Main transfer record
        Transfer transfer = new Transfer();
        transfer.setTransferId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transfer.setFromAccount(payerAccount);
        transfer.setToAccount(requesterAccount);
        transfer.setAmount(amount);
        transfer.setDescription(request.getDescription() + " (Approved Request: " + request.getRequestId() + ")");
        transfer.setStatus(Transfer.Status.COMPLETED);
        transfer.setCompletedTime(LocalDateTime.now());
        em.persist(transfer);

        // Route fee to admin
        Account adminAccount = findAdminAccount();
        if (adminAccount != null) {
            adminAccount.setBalance(adminAccount.getBalance().add(fee));
            em.merge(adminAccount);

            Transfer feeTransfer = new Transfer();
            feeTransfer.setTransferId("FEE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            feeTransfer.setFromAccount(payerAccount);
            feeTransfer.setToAccount(adminAccount);
            feeTransfer.setAmount(fee);
            feeTransfer.setDescription("Transaction Fee 2.45% for " + transfer.getTransferId());
            feeTransfer.setStatus(Transfer.Status.COMPLETED);
            feeTransfer.setCompletedTime(LocalDateTime.now());
            em.persist(feeTransfer);
        } else {
            logger.warning("No active admin account — fee $" + fee + " could not be routed.");
        }

        request.setStatus(MoneyRequest.Status.APPROVED);
        em.merge(request);

        notificationService.notifyUser(requesterAccount.getOwner().getId(),
            payerAccount.getOwner().getName() + " approved your request. Received $" + amount + ".");
        notificationService.notifyUser(payerAccount.getOwner().getId(),
            "Approved payment of $" + amount + " to " + requesterAccount.getOwner().getName() +
            ". Fee charged: $" + fee + ".");
    }

    // -------------------------------------------------------------------------
    // Reject / decline a request
    // -------------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void rejectRequest(Long requestId, String payerEmail) throws BusinessRuleViolationException {
        MoneyRequest request = em.find(MoneyRequest.class, requestId);
        if (request == null) throw new BusinessRuleViolationException("Money request not found.");
        if (request.getStatus() != MoneyRequest.Status.PENDING)
            throw new BusinessRuleViolationException("This request has already been processed.");

        Account payerAccount = request.getPayer();
        if (!payerAccount.getOwner().getEmail().equals(payerEmail))
            throw new SecurityException("Unauthorized: You do not own the account requested to pay.");

        request.setStatus(MoneyRequest.Status.REJECTED);
        em.merge(request);

        notificationService.notifyUser(request.getRequester().getOwner().getId(),
            payerAccount.getOwner().getName() + " declined your request for $" + request.getAmount() + ".");
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<MoneyRequest> getIncomingPendingRequests(String payerEmail) {
        return em.createQuery(
            "SELECT r FROM MoneyRequest r WHERE r.payer.owner.email = :email AND r.status = :status ORDER BY r.id DESC",
            MoneyRequest.class)
            .setParameter("email", payerEmail)
            .setParameter("status", MoneyRequest.Status.PENDING)
            .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<MoneyRequest> getOutgoingRequests(String requesterEmail) {
        return em.createQuery(
            "SELECT r FROM MoneyRequest r WHERE r.requester.owner.email = :email ORDER BY r.id DESC",
            MoneyRequest.class)
            .setParameter("email", requesterEmail)
            .getResultList();
    }

    /** All money requests involving the user's account (both sides), for Home feed */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<MoneyRequest> getAllRequests(String userEmail) {
        return em.createQuery(
            "SELECT r FROM MoneyRequest r WHERE r.requester.owner.email = :email " +
            "OR r.payer.owner.email = :email ORDER BY r.id DESC",
            MoneyRequest.class)
            .setParameter("email", userEmail)
            .getResultList();
    }
}
