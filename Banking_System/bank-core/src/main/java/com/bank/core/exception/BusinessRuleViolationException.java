package com.bank.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class BusinessRuleViolationException extends Exception {
    
    private String violationType;

    public BusinessRuleViolationException(String type) {
        super("Business rule violation: " + type);
        this.violationType = type;
    }

    public String getViolationType() {
        return violationType;
    }
}
