package com.techmart.service;

import com.techmart.model.Order;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class OrderManagementService {

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    public List<Order> getAllOrders() {
        return em.createQuery(
                "SELECT o FROM Order o ORDER BY o.createdAt DESC", Order.class)
                .getResultList();
    }

    public Order getOrderById(String id) {
        return em.find(Order.class, id);
    }

    public com.techmart.model.Product getProductById(String id) {
        return em.find(com.techmart.model.Product.class, id);
    }

    public com.techmart.user.User getCustomerByEmail(String email) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", com.techmart.user.User.class)
                .setParameter("email", email)
                .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public List<Order> getOrdersByEmail(String email) {
        return em.createQuery(
                "SELECT o FROM Order o WHERE o.customerEmail = :email ORDER BY o.createdAt DESC",
                Order.class)
                .setParameter("email", email)
                .getResultList();
    }

    public Order updateOrderStatus(String id, String status) {
        Order order = getOrderById(id);
        if (order == null) {
            return null;
        }
        String oldStatus = order.getStatus();
        order.setStatus(status);
        
        if ("CANCELLED".equalsIgnoreCase(status) && !"CANCELLED".equalsIgnoreCase(oldStatus)) {
            com.techmart.model.Product product = em.find(com.techmart.model.Product.class, order.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + order.getQuantity());
                em.merge(product);
            }
        } else if (!"CANCELLED".equalsIgnoreCase(status) && "CANCELLED".equalsIgnoreCase(oldStatus)) {
            com.techmart.model.Product product = em.find(com.techmart.model.Product.class, order.getProductId());
            if (product != null) {
                product.setStock(Math.max(0, product.getStock() - order.getQuantity()));
                em.merge(product);
            }
        }
        
        return em.merge(order);
    }

    public boolean deleteOrder(String id) {
        Order order = getOrderById(id);
        if (order == null) {
            return false;
        }
        em.remove(em.contains(order) ? order : em.merge(order));
        return true;
    }
}