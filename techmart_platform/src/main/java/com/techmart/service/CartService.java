package com.techmart.service;

import com.techmart.model.CartItem;
import com.techmart.model.Product;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Stateless
public class CartService {

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    public Map<String, Integer> getCartMap(String customerEmail) {
        Map<String, Integer> cart = new LinkedHashMap<>();
        for (CartItem item : getCartItems(customerEmail)) {
            cart.put(item.getProductId(), item.getQuantity());
        }
        return cart;
    }

    public List<CartItem> getCartItems(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            return List.of();
        }
        TypedQuery<CartItem> query = em.createQuery(
            "SELECT c FROM CartItem c WHERE c.customerEmail = :email ORDER BY c.updatedAt DESC, c.createdAt DESC",
            CartItem.class
        );
        query.setParameter("email", customerEmail);
        return query.getResultList();
    }

    public void addToCart(String customerEmail, String productId, int quantity) {
        if (isBlank(customerEmail) || isBlank(productId) || quantity <= 0) {
            return;
        }

        Product product = em.find(Product.class, productId);
        int stock = (product != null) ? product.getStock() : 0;

        CartItem item = findByCustomerAndProduct(customerEmail, productId);
        int currentQty = (item != null) ? item.getQuantity() : 0;
        int targetQty = currentQty + quantity;

        if (targetQty > stock) {
            if (item != null) {
                em.remove(em.contains(item) ? item : em.merge(item));
            }
            throw new IllegalArgumentException("Failed to add: Exceeded stock!");
        }

        LocalDateTime now = LocalDateTime.now();
        if (item == null) {
            item = new CartItem(UUID.randomUUID().toString(), customerEmail, productId, quantity, now, now);
            em.persist(item);
        } else {
            item.setQuantity(targetQty);
            item.setUpdatedAt(now);
            em.merge(item);
        }
    }

    public void setQuantity(String customerEmail, String productId, int quantity) {
        if (isBlank(customerEmail) || isBlank(productId)) {
            return;
        }
        if (quantity <= 0) {
            removeItem(customerEmail, productId);
            return;
        }

        Product product = em.find(Product.class, productId);
        int stock = (product != null) ? product.getStock() : 0;

        if (quantity > stock) {
            CartItem item = findByCustomerAndProduct(customerEmail, productId);
            if (item != null) {
                em.remove(em.contains(item) ? item : em.merge(item));
            }
            throw new IllegalArgumentException("Failed to update: Exceeded stock!");
        }

        CartItem item = findByCustomerAndProduct(customerEmail, productId);
        LocalDateTime now = LocalDateTime.now();
        if (item == null) {
            item = new CartItem(UUID.randomUUID().toString(), customerEmail, productId, quantity, now, now);
            em.persist(item);
        } else {
            item.setQuantity(quantity);
            item.setUpdatedAt(now);
            em.merge(item);
        }
    }

    public void removeItem(String customerEmail, String productId) {
        CartItem item = findByCustomerAndProduct(customerEmail, productId);
        if (item != null) {
            em.remove(em.contains(item) ? item : em.merge(item));
        }
    }

    public void clearCart(String customerEmail) {
        if (isBlank(customerEmail)) {
            return;
        }
        em.createQuery("DELETE FROM CartItem c WHERE c.customerEmail = :email")
            .setParameter("email", customerEmail)
            .executeUpdate();
    }

    private CartItem findByCustomerAndProduct(String customerEmail, String productId) {
        TypedQuery<CartItem> query = em.createQuery(
            "SELECT c FROM CartItem c WHERE c.customerEmail = :email AND c.productId = :productId",
            CartItem.class
        );
        query.setParameter("email", customerEmail);
        query.setParameter("productId", productId);
        List<CartItem> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}