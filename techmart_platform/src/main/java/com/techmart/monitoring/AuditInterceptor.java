package com.techmart.monitoring;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.security.enterprise.SecurityContext;
import jakarta.inject.Inject;
import java.util.logging.Logger;

@Interceptor
@AuditLog
public class AuditInterceptor {
    
    private static final Logger auditLogger = Logger.getLogger(AuditInterceptor.class.getName());
    
    @Inject
    private SecurityContext securityContext;

    @AroundInvoke
    public Object logAudit(InvocationContext ctx) throws Exception {
        String user = "Anonymous";
        if (securityContext != null && securityContext.getCallerPrincipal() != null) {
            user = securityContext.getCallerPrincipal().getName();
        }
        
        String method = ctx.getMethod().getName();
        long start = System.currentTimeMillis();
        
        try {
            Object result = ctx.proceed();
            auditLogger.info(String.format("User: %s, Method: %s, Duration: %dms, Status: SUCCESS", 
                user, method, System.currentTimeMillis() - start));
            return result;
        } catch (Exception e) {
            auditLogger.severe(String.format("User: %s, Method: %s, Error: %s", 
                user, method, e.getMessage()));
            throw e;
        }
    }
}
