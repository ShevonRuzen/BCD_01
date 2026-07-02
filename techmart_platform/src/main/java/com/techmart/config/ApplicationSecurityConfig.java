package com.techmart.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;

@BasicAuthenticationMechanismDefinition(
    realmName = "TechMartRealm"
)
@ApplicationScoped
public class ApplicationSecurityConfig {
    
    @PostConstruct
    public void init() {
        // Configure security constraints
    }
}
