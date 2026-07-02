package com.techmart.order;

import com.techmart.inventory.InventoryCache;
import com.techmart.monitoring.PerformanceMetricsCollector;
import com.techmart.model.Order;
import com.techmart.model.Product;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@Stateless
public class OrderProcessingEngine {

    private static final Logger LOGGER = Logger.getLogger(OrderProcessingEngine.class.getName());

    @EJB
    private InventoryCache inventoryCache;

    @EJB
    private PerformanceMetricsCollector metricsCollector;

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @EJB
    private com.techmart.order.AsynchronousNotificationEngine notificationEngine;

    public OrderResult processCheckout(String productId, int quantity, String customerEmail) {
        Map<String, Integer> singleItemCart = new LinkedHashMap<>();
        singleItemCart.put(productId, quantity);
        return processCartCheckout(singleItemCart, customerEmail);
    }

    public OrderResult processCartCheckout(Map<String, Integer> cart, String customerEmail) {
        long startTime = System.nanoTime();

        if (cart == null || cart.isEmpty() || customerEmail == null || customerEmail.isBlank()) {
            double duration = calculateDuration(startTime);
            metricsCollector.recordCheckout(false, duration);
            return new OrderResult(false, null, "INVALID_CHECKOUT", duration);
        }

        Map<String, Product> productsById = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue() == null ? 0 : entry.getValue();
            if (productId == null || productId.isBlank() || quantity <= 0) {
                double duration = calculateDuration(startTime);
                metricsCollector.recordCheckout(false, duration);
                return new OrderResult(false, null, "INVALID_CHECKOUT", duration);
            }

            Product product = em.find(Product.class, productId);
            if (product == null) {
                double duration = calculateDuration(startTime);
                metricsCollector.recordCheckout(false, duration);
                LOGGER.warning("Checkout failed: Product " + productId + " not found.");
                return new OrderResult(false, null, "PRODUCT_NOT_FOUND", duration);
            }

            if (product.getStock() < quantity) {
                double duration = calculateDuration(startTime);
                metricsCollector.recordCheckout(false, duration);
                LOGGER.warning("Checkout failed: Product " + productId + " out of stock.");
                return new OrderResult(false, null, "OUT_OF_STOCK", duration);
            }

            productsById.put(productId, product);
        }

        String firstOrderId = null;
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue();
            Product product = productsById.get(productId);
            product.setStock(product.getStock() - quantity);
            em.merge(product);
            if (inventoryCache != null) {
                inventoryCache.setStock(productId, product.getStock());
            }

            String orderId = UUID.randomUUID().toString();
            Order order = new Order(orderId, productId, quantity, customerEmail, "PENDING", LocalDateTime.now());
            em.persist(order);
            LOGGER.info("Persisted order " + orderId + " to DB.");

            if (firstOrderId == null) {
                firstOrderId = orderId;
            }

            if (notificationEngine != null) {
                notificationEngine.sendEmailConfirmation(customerEmail, orderId);
            }
        }

        double duration = calculateDuration(startTime);
        metricsCollector.recordCheckout(true, duration);
        return new OrderResult(true, firstOrderId, "SUCCESS", duration);
    }

    private double calculateDuration(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000.0;
    }
}