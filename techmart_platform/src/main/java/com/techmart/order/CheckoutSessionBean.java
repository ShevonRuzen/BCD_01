package com.techmart.order;

import com.techmart.monitoring.PerformanceMetricsCollector;
import com.techmart.inventory.InventoryCache;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Stateful;
import jakarta.ejb.Remove;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Stateful
public class CheckoutSessionBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(CheckoutSessionBean.class.getName());

    @EJB
    private PerformanceMetricsCollector metricsCollector;

    @EJB
    private OrderProcessingEngine orderEngine;

    @EJB
    private InventoryCache inventoryCache;

    private String customerEmail;
    private Map<String, Integer> cart = new HashMap<>();

    @PostConstruct
    public void init() {
        LOGGER.info("Stateful Session Bean Created: CheckoutSessionBean initialization.");
        metricsCollector.incrementActiveSessions();
    }

    public void setCustomerEmail(String email) {
        this.customerEmail = email;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void addToCart(String productId, int quantity) {
        if (productId == null || productId.trim().isEmpty() || quantity <= 0) {
            return;
        }
        cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
    }

    public void setCartQuantity(String productId, int quantity) {
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }
        if (quantity <= 0) {
            cart.remove(productId);
            return;
        }
        cart.put(productId, quantity);
    }

    public void clearCart() {
        cart.clear();
    }

    public void removeFromCart(String productId) {
        cart.remove(productId);
    }

    public Map<String, Integer> getCart() {
        return cart;
    }

    public OrderResult checkout() {
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            return new OrderResult(false, null, "MISSING_EMAIL", 0.0);
        }
        if (cart.isEmpty()) {
            return new OrderResult(false, null, "CART_EMPTY", 0.0);
        }

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            int availableStock = inventoryCache != null ? inventoryCache.getStock(entry.getKey()) : 0;
            if (availableStock < entry.getValue()) {
                return new OrderResult(false, null, "OUT_OF_STOCK", 0.0);
            }
        }

        String firstOrderId = null;
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            OrderResult result = orderEngine.processCheckout(entry.getKey(), entry.getValue(), customerEmail);
            if (!result.isSuccess()) {
                return result;
            }
            if (firstOrderId == null) {
                firstOrderId = result.getOrderId();
            }
        }
        cart.clear();
        return new OrderResult(true, firstOrderId, "SUCCESS", 0.0);
    }

    @Remove
    public void endSession() {
        LOGGER.info("Stateful Session Bean @Remove: Ending session.");
    }

    @PreDestroy
    public void destroy() {
        LOGGER.info("Stateful Session Bean @PreDestroy: Cleaning resources.");
        metricsCollector.decrementActiveSessions();
    }

    @PrePassivate
    public void passivate() {
        LOGGER.info("Stateful Session Bean @PrePassivate: Passivating session state.");
    }

    @PostActivate
    public void activate() {
        LOGGER.info("Stateful Session Bean @PostActivate: Activating session state.");
    }
}
