package com.bank.ejb.security;

import com.bank.ejb.service.AuditLogSessionBean;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.security.Principal;
import java.util.Arrays;
import java.util.logging.Logger;

@SecurityAudit
@Interceptor
public class SecurityInterceptor {

    private static final Logger logger = Logger.getLogger(SecurityInterceptor.class.getName());

    @Inject
    private Principal principal;

    @Inject
    private AuditLogSessionBean auditLogService;

    @AroundInvoke
    public Object auditSecurity(InvocationContext ctx) throws Exception {
        String caller = "UNKNOWN";
        try {
            if (principal != null && principal.getName() != null) {
                caller = principal.getName();
            }
        } catch (Exception e) {
            // Context not initialized
        }
        
        long start = System.currentTimeMillis();
        String target = ctx.getTarget().getClass().getSimpleName();
        String method = ctx.getMethod().getName();
        String params = Arrays.toString(ctx.getParameters());
        
        logger.info(String.format("Security Audit: [%s] invoking %s.%s", caller, target, method));
        
        boolean success = false;
        try {
            Object result = ctx.proceed();
            long duration = System.currentTimeMillis() - start;
            success = true;
            
            // Persist successful log entry in a separate transaction
            auditLogService.log(caller, target + "." + method, "Parameters: " + params, duration, success);
            
            logger.info(String.format("Security Audit: [%s] completed %s.%s in %d ms", caller, target, method, duration));
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            
            // Persist failed log entry in a separate transaction
            auditLogService.log(caller, target + "." + method, 
                               "Failed with: " + e.getClass().getSimpleName() + " - Parameters: " + params, 
                               duration, success);
            
            logger.severe(String.format("Security Audit: [%s] failed %s.%s with exception %s", caller, target, method, e.getClass().getName()));
            throw e;
        }
    }
}
