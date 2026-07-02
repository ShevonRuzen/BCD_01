package com.techmart.user;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Logger;

@Stateless
public class UserAccountService {

    private static final Logger LOGGER = Logger.getLogger(UserAccountService.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    public boolean registerUser(String username, String email, String password) {
        if (isBlank(username) || isBlank(email) || isBlank(password)) {
            return false;
        }
        if (emailExists(email) || usernameExists(username)) {
            return false;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(password);
        user.setRole("CUSTOMER");
        user.setActive(true);
        em.persist(user);
        LOGGER.info("Registered new user: " + email);
        return true;
    }

    public User authenticate(String email, String password) {
        if (isBlank(email) || isBlank(password)) {
            return null;
        }

        TypedQuery<User> query = em.createQuery(
            "SELECT u FROM User u WHERE u.email = :email", User.class
        );
        query.setParameter("email", email);
        List<User> users = query.getResultList();

        if (users.isEmpty()) {
            return null;
        }

        User user = users.get(0);
        if (!user.isActive()) {
            return null;
        }
        if (password.equals(user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    public User updateUser(String id, String username, String email, String role) {
        return updateUser(id, username, email, role, null, null, null);
    }

    public User updateUser(String id, String username, String email, String role, Boolean active) {
        return updateUser(id, username, email, role, active, null, null);
    }

    public User updateUser(String id, String username, String email, String role, Boolean active, String address, String telephone) {
        User user = findById(id);
        if (user == null) {
            return null;
        }
        if (username != null && !username.isBlank()) {
            user.setUsername(username);
        }
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }
        if (role != null && !role.isBlank()) {
            user.setRole(role);
        }
        if (active != null) {
            user.setActive(active);
        }
        user.setAddress(address);
        user.setTelephone(telephone);
        return em.merge(user);
    }

    public User updateProfile(User user) {
        return em.merge(user);
    }

    public boolean emailExists(String email) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class
        );
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }

    public boolean usernameExists(String username) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class
        );
        query.setParameter("username", username);
        return query.getSingleResult() > 0;
    }

    public User findByEmail(String email) {
        TypedQuery<User> query = em.createQuery(
            "SELECT u FROM User u WHERE u.email = :email", User.class
        );
        query.setParameter("email", email);
        List<User> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public User findById(String id) {
        return em.find(User.class, id);
    }

    public List<User> getAllUsers() {
        return em.createQuery("SELECT u FROM User u ORDER BY u.username", User.class)
                .getResultList();
    }

    public boolean deleteUser(String id) {
        User user = findById(id);
        if (user == null) {
            return false;
        }
        em.remove(em.contains(user) ? user : em.merge(user));
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}