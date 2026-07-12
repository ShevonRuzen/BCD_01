package com.bank.ejb.service;

import com.bank.core.entity.User;
import com.bank.core.exception.BusinessRuleViolationException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;

@Stateless
public class UserSessionBean {

    @PersistenceContext(unitName = "BankPU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User registerUser(String name, String email, String password, User.UserType type) throws BusinessRuleViolationException {
        try {
            // Check if email exists
            em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
              .setParameter("email", email)
              .getSingleResult();
            throw new BusinessRuleViolationException("Email already in use");
        } catch (NoResultException e) {
            // Proceed
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        // Extremely simple hashing for demonstration; in production use Argon2/BCrypt
        user.setPasswordHash(hashPassword(password));
        
        // Auto-bootstrap: if this is the first user in the DB, make them an ADMIN
        Long userCount = em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
        if (userCount == 0) {
            user.setUserType(User.UserType.ADMIN);
        } else {
            user.setUserType(type != null ? type : User.UserType.USER);
        }
        
        em.persist(user);
        return user;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User createAdmin(String name, String email, String password) throws BusinessRuleViolationException {
        return registerUser(name, email, password, User.UserType.ADMIN);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User authenticate(String email, String password) {
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                          .setParameter("email", email)
                          .getSingleResult();
            if (user.getPasswordHash().equals(hashPassword(password))) {
                return user;
            }
        } catch (NoResultException e) {
            // Not found
        }
        return null;
    }
    
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findByEmail(String email) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                     .setParameter("email", email)
                     .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User updateProfile(Long userId, String newName, String newPassword) throws BusinessRuleViolationException {
        User user = em.find(User.class, userId);
        if (user == null) {
            throw new BusinessRuleViolationException("User not found.");
        }
        if (newName != null && !newName.trim().isEmpty()) {
            user.setName(newName);
        }
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (newPassword.length() < 8) {
                throw new BusinessRuleViolationException("Password must be at least 8 characters.");
            }
            user.setPasswordHash(hashPassword(newPassword));
        }
        return em.merge(user);
    }
    
    private String hashPassword(String password) {
        // Dummy hashing. E.g. SHA-256 in real system if not using BCrypt library
        return String.valueOf(password.hashCode());
    }
}
