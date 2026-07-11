package com.techmart.messaging;

import com.techmart.model.Order;
import com.techmart.model.Product;
import com.techmart.monitoring.PerformanceMetricsCollector;
import com.techmart.order.AsynchronousNotificationEngine;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.logging.Logger;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:app/jms/OrderQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "maxPoolSize", propertyValue = "100"),
        @ActivationConfigProperty(propertyName = "poolSize", propertyValue = "20")
})
public class OrderProcessingMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(OrderProcessingMDB.class.getName());

    @PersistenceContext(unitName = "TechMartPU")
    private EntityManager em;

    @EJB
    private PerformanceMetricsCollector metricsCollector;

    @EJB
    private AsynchronousNotificationEngine notificationEngine;

    @EJB
    private com.techmart.messaging.NotificationProducer notificationProducer;

    @Override
    public void onMessage(Message message) {
        long dbStart = System.nanoTime();
        try {
            if (message instanceof TextMessage) {
                String orderPayload = ((TextMessage) message).getText();
                LOGGER.info("MDB received incoming queue message: " + orderPayload);
                // Parse JSON payload
                JsonObject json = Json.createReader(new StringReader(orderPayload)).readObject();
                String orderId = json.getString("orderId");
                String productId = json.getString("productId");
                int quantity = json.getInt("quantity");
                String email = json.getString("email");
                // 1. Update Product stock
                Product product = em.find(Product.class, productId);
                if (product != null) {
                    product.setStock(product.getStock() - quantity);
                    em.merge(product);
                    LOGGER.info("MDB updated product " + productId + " stock in DB.");
                } else {
                    LOGGER.warning("MDB could not find product " + productId + " in DB.");
                }
                // 2. Persist new Order
                Order order = new Order(orderId, productId, quantity, email, "PROCESSED", LocalDateTime.now());
                em.persist(order);
                LOGGER.info("MDB persisted order " + orderId + " to DB.");
                // 3. Measure DB duration
                double dbDurationMs = (System.nanoTime() - dbStart) / 1_000_000.0;
                metricsCollector.recordDbPersistence(dbDurationMs);
                // 4. Async notification
                notificationEngine.sendEmailConfirmation(email, orderId);
                // 5. JMS notifications
                notificationProducer.sendNotification(
                    "New Order Placed",
                    "Order " + orderId + " was placed for product " + productId + " (qty: " + quantity + ") by " + email,
                    "ADMIN"
                );
                notificationProducer.sendNotification(
                    "Order Processed",
                    "Your order " + orderId + " has been successfully processed.",
                    email
                );
            }
        } catch (Exception e) {
            LOGGER.severe("MDB message worker encountered execution error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}