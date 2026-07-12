package com.bank.ejb.service;

import com.bank.core.entity.AuditLog;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;

@Stateless
public class AuditLogSessionBean {

    @PersistenceContext(unitName = "BankPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void log(String caller, String action, String details, long durationMs, boolean success) {
        AuditLog auditLog = new AuditLog();
        auditLog.setCaller(caller);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setDurationMs(durationMs);
        auditLog.setSuccess(success);
        auditLog.setCreatedAt(LocalDateTime.now());
        
        em.persist(auditLog);
    }
}
