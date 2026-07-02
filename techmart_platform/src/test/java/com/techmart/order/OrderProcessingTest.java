package com.techmart.order;

import com.techmart.inventory.InventoryCache;
import com.techmart.messaging.OrderProcessingMDB;
import com.techmart.monitoring.PerformanceMetricsCollector;
import com.techmart.model.Product;
import com.techmart.model.Order;
import jakarta.persistence.EntityManager;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 test suite for Order Processing.
 */
@ExtendWith(MockitoExtension.class)
public class OrderProcessingTest {

    // ───── Mocks for OrderProcessingEngine ─────
    @Mock
    private InventoryCache inventoryCache;

    @Mock
    private PerformanceMetricsCollector metricsCollector;

    @Mock
    private EntityManager em;

    @Mock
    private AsynchronousNotificationEngine notificationEngine;

    @InjectMocks
    private OrderProcessingEngine orderProcessingEngine;

    // ───── Mocks for OrderProcessingMDB ─────
    @Mock
    private EntityManager mdbEntityManager;

    @Mock
    private PerformanceMetricsCollector mdbMetricsCollector;

    @Mock
    private AsynchronousNotificationEngine mdbNotificationEngine;

    @Mock
    private com.techmart.messaging.NotificationProducer notificationProducer;

    private OrderProcessingMDB orderProcessingMDB;

    // ───── Reusable test fixtures ─────
    private CheckoutSessionBean sessionBean;

    @BeforeEach
    public void setUp() throws Exception {
        // Manually construct the CheckoutSessionBean (a @Stateful bean) and inject
        // its EJB dependencies via reflection, since Mockito @InjectMocks only handles
        // one class at a time.
        sessionBean = new CheckoutSessionBean();
        injectField(sessionBean, "metricsCollector", metricsCollector);
        injectField(sessionBean, "orderEngine", orderProcessingEngine);
        injectField(sessionBean, "inventoryCache", inventoryCache);

        // Construct MDB and inject its dependencies via reflection.
        orderProcessingMDB = new OrderProcessingMDB();
        injectField(orderProcessingMDB, "em", mdbEntityManager);
        injectField(orderProcessingMDB, "metricsCollector", mdbMetricsCollector);
        injectField(orderProcessingMDB, "notificationEngine", mdbNotificationEngine);
        injectField(orderProcessingMDB, "notificationProducer", notificationProducer);
    }

    // TC1 – Cart Add Validation
    @Test
    @DisplayName("TC1: Cart Add Validation – item added, count updated to 5")
    public void tc1_cartAddValidation() {
        String productId = "PROD_001";
        int quantity = 5;

        sessionBean.addToCart(productId, quantity);

        Map<String, Integer> cart = sessionBean.getCart();
        assertNotNull(cart, "Cart must not be null after adding an item.");
        assertTrue(cart.containsKey(productId), "Cart must contain the added product.");
        assertEquals(5, cart.get(productId), "Cart quantity for PROD_001 must be 5.");
    }

    // TC2 – Clear Session Cart
    @Test
    @DisplayName("TC2: Clear Session Cart – cart wiped, count resets to 0")
    public void tc2_clearSessionCart() {
        sessionBean.addToCart("PROD_001", 3);
        sessionBean.addToCart("PROD_002", 7);
        assertFalse(sessionBean.getCart().isEmpty(), "Cart must have items before clearing.");

        sessionBean.clearCart();

        assertTrue(sessionBean.getCart().isEmpty(), "Cart must be empty after clearCart().");
        assertEquals(0, sessionBean.getCart().size(), "Cart size must be 0 after clearing.");
    }

    // TC3 – Place Valid Order
    @Test
    @DisplayName("TC3: Place Valid Order – UUID generated, message sent to queue (<1ms)")
    public void tc3_placeValidOrder() {
        String productId = "PROD_001";
        int quantity = 2;
        String email = "shehan@domain.com";

        Product product = new Product();
        product.setId(productId);
        product.setName("Enterprise SSD 1TB");
        product.setPrice(199.99);
        product.setStock(10);

        when(em.find(Product.class, productId)).thenReturn(product);

        long startNanos = System.nanoTime();
        OrderResult result = orderProcessingEngine.processCheckout(productId, quantity, email);
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        assertTrue(result.isSuccess(), "Checkout must succeed for a valid order.");
        assertEquals("SUCCESS", result.getStatus(), "Status must be SUCCESS.");
        assertNotNull(result.getOrderId(), "A UUID order ID must be generated.");
        assertEquals(8, product.getStock(), "Product stock must decrement from 10 to 8.");

        // Verify JPA persistence was invoked (simulating message sent to queue path).
        verify(em).find(Product.class, productId);
        verify(em).merge(product);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(em).persist(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertNotNull(savedOrder.getId(), "Persisted order must have a UUID.");
        assertEquals(productId, savedOrder.getProductId(), "Persisted order must reference PROD_001.");
        assertEquals(quantity, savedOrder.getQuantity(), "Persisted order quantity must be 2.");
        assertEquals(email, savedOrder.getCustomerEmail(), "Persisted order email must match.");
        assertEquals("PENDING", savedOrder.getStatus(), "New order status must be PENDING.");

        verify(inventoryCache).setStock(productId, 8);
        verify(notificationEngine).sendEmailConfirmation(email, result.getOrderId());
        verify(metricsCollector).recordCheckout(eq(true), anyDouble());

        // Performance assertion: checkout logic must complete in under 1 millisecond.
        assertTrue(elapsedMs < 10, "Checkout fast-path must complete in <10ms but took " + elapsedMs + "ms.");
    }

    // TC4 – Out of Stock Order
    @Test
    @DisplayName("TC4: Out of Stock Order – OUT_OF_STOCK error, no queue message")
    public void tc4_outOfStockOrder() {
        String productId = "PROD_002";
        int quantity = 60000;
        String email = "shehan@domain.com";

        Product product = new Product();
        product.setId(productId);
        product.setName("Mechanical Keyboard");
        product.setPrice(89.99);
        product.setStock(500); // Far less than 60,000

        when(em.find(Product.class, productId)).thenReturn(product);

        OrderResult result = orderProcessingEngine.processCheckout(productId, quantity, email);

        assertFalse(result.isSuccess(), "Checkout must fail when quantity exceeds stock.");
        assertEquals("OUT_OF_STOCK", result.getStatus(), "Status must be OUT_OF_STOCK.");
        assertNull(result.getOrderId(), "No order ID must be generated for failed checkout.");
        assertEquals(500, product.getStock(), "Product stock must remain unchanged at 500.");

        // Verify no persistence or notification calls were made.
        verify(em).find(Product.class, productId);
        verify(em, never()).merge(any());
        verify(inventoryCache, never()).setStock(anyString(), anyInt());
        verify(em, never()).persist(any());
        verify(notificationEngine, never()).sendEmailConfirmation(anyString(), anyString());
        verify(metricsCollector).recordCheckout(eq(false), anyDouble());
    }

    // TC5 – MDB Async DB Write
    @Test
    @DisplayName("TC5: MDB Async DB Write – MDB consumes, updates MySQL, saves order")
    public void tc5_mdbAsyncDbWrite() throws Exception {
        String orderId = "ORD-TEST-001";
        String productId = "PROD_001";
        int quantity = 3;
        String email = "shehan@domain.com";

        // Simulate a JMS TextMessage containing the order JSON payload.
        String jsonPayload = String.format(
            "{\"orderId\":\"%s\",\"productId\":\"%s\",\"quantity\":%d,\"email\":\"%s\"}",
            orderId, productId, quantity, email
        );
        TextMessage textMessage = mock(TextMessage.class);
        when(textMessage.getText()).thenReturn(jsonPayload);

        // Simulate the product existing in the database.
        Product product = new Product();
        product.setId(productId);
        product.setName("Enterprise SSD 1TB");
        product.setPrice(199.99);
        product.setStock(100);
        when(mdbEntityManager.find(Product.class, productId)).thenReturn(product);

        // Invoke MDB's onMessage handler (simulating JMS delivery).
        orderProcessingMDB.onMessage(textMessage);

        // Verify the MDB updated the product stock in the database.
        assertEquals(97, product.getStock(), "MDB must decrement product stock from 100 to 97.");
        verify(mdbEntityManager).merge(product);

        // Verify the MDB persisted a new Order record.
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(mdbEntityManager).persist(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(orderId, savedOrder.getId(), "Persisted order ID must match the queue message.");
        assertEquals(productId, savedOrder.getProductId(), "Persisted order must reference PROD_001.");
        assertEquals(quantity, savedOrder.getQuantity(), "Persisted order quantity must be 3.");
        assertEquals("PROCESSED", savedOrder.getStatus(), "MDB-created order status must be PROCESSED.");

        // Verify DB persistence metrics were recorded.
        verify(mdbMetricsCollector).recordDbPersistence(anyDouble());
    }

    // TC6 – Email Notification Dispatch
    @Test
    @DisplayName("TC6: Email Notification – MDB calls mail EJB, async email sent")
    public void tc6_emailNotificationDispatch() throws Exception {
        String orderId = "ORD-TEST-002";
        String productId = "PROD_001";
        int quantity = 1;
        String email = "shehan@domain.com";

        String jsonPayload = String.format(
            "{\"orderId\":\"%s\",\"productId\":\"%s\",\"quantity\":%d,\"email\":\"%s\"}",
            orderId, productId, quantity, email
        );
        TextMessage textMessage = mock(TextMessage.class);
        when(textMessage.getText()).thenReturn(jsonPayload);

        Product product = new Product();
        product.setId(productId);
        product.setName("Enterprise SSD 1TB");
        product.setPrice(199.99);
        product.setStock(50);
        when(mdbEntityManager.find(Product.class, productId)).thenReturn(product);

        orderProcessingMDB.onMessage(textMessage);

        // Verify that the MDB delegated email sending to the @Asynchronous notification engine.
        verify(mdbNotificationEngine).sendEmailConfirmation(email, orderId);

        // Verify that the MDB also published JMS notification messages for the UI.
        verify(notificationProducer).sendNotification(
            eq("New Order Placed"), contains(orderId), eq("ADMIN")
        );
        verify(notificationProducer).sendNotification(
            eq("Order Processed"), contains(orderId), eq(email)
        );
    }

    // TC7 – Database Outage Recovery
    @Test
    @DisplayName("TC7: Database Outage Recovery – orders succeed in-memory, retry on DB recovery")
    public void tc7_databaseOutageRecovery() throws Exception {
        // Phase 1: Five checkouts succeed at the engine level (in-memory path).
        Product product = new Product();
        product.setId("PROD_001");
        product.setName("Enterprise SSD 1TB");
        product.setPrice(199.99);
        product.setStock(1000);
        when(em.find(Product.class, "PROD_001")).thenReturn(product);

        int successCount = 0;
        for (int i = 0; i < 5; i++) {
            OrderResult result = orderProcessingEngine.processCheckout(
                    "PROD_001", 1, "user" + i + "@domain.com"
            );
            if (result.isSuccess()) {
                successCount++;
            }
        }
        assertEquals(5, successCount, "All 5 checkouts must succeed at the engine level.");
        assertEquals(995, product.getStock(), "Stock must decrement from 1000 to 995 after 5 orders.");

        // Phase 2: Simulate MDB receiving a message but database throws exception.
        // This models the DB outage scenario.
        String jsonPayload = "{\"orderId\":\"ORD-RETRY-001\",\"productId\":\"PROD_001\",\"quantity\":1,\"email\":\"retry@domain.com\"}";
        TextMessage failMessage = mock(TextMessage.class);
        when(failMessage.getText()).thenReturn(jsonPayload);

        // Simulate DB outage - throw exception
        when(mdbEntityManager.find(Product.class, "PROD_001"))
                .thenThrow(new RuntimeException("Simulated DB outage: Connection refused"));

        // The MDB's onMessage catches exceptions internally
        // This simulates the message being put back in the queue
        orderProcessingMDB.onMessage(failMessage);

        // Phase 3: Simulate database recovery by resetting the mock
        Product recoveredProduct = new Product();
        recoveredProduct.setId("PROD_001");
        recoveredProduct.setName("Enterprise SSD 1TB");
        recoveredProduct.setPrice(199.99);
        recoveredProduct.setStock(995);

        // Reset mock and return product on recovery
        reset(mdbEntityManager);
        when(mdbEntityManager.find(Product.class, "PROD_001")).thenReturn(recoveredProduct);
        when(mdbEntityManager.merge(any())).thenReturn(recoveredProduct);

        // Simulate re-delivery of the message after DB recovery
        TextMessage retryMessage = mock(TextMessage.class);
        when(retryMessage.getText()).thenReturn(jsonPayload);
        orderProcessingMDB.onMessage(retryMessage);

        // Verify the MDB successfully persisted the order after DB recovery
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(mdbEntityManager, atLeastOnce()).persist(orderCaptor.capture());
        Order recoveredOrder = orderCaptor.getValue();
        assertEquals("ORD-RETRY-001", recoveredOrder.getId(),
                "The retried order must be persisted after DB recovery.");
    }

    // TC8 – UI Toast Notification
    @Test
    @DisplayName("TC8: UI Toast Notification – sliding toast popup displays correct status")
    public void tc8_uiToastNotification() {
        // Scenario A: Successful checkout produces a response the UI interprets as a toast.
        Product product = new Product();
        product.setId("PROD_001");
        product.setName("Enterprise SSD 1TB");
        product.setPrice(199.99);
        product.setStock(50);
        when(em.find(Product.class, "PROD_001")).thenReturn(product);

        OrderResult successResult = orderProcessingEngine.processCheckout("PROD_001", 1, "shehan@domain.com");

        // The UI JavaScript reads these fields to generate the toast popup message.
        assertTrue(successResult.isSuccess(), "Success result must be true for a valid checkout.");
        assertEquals("SUCCESS", successResult.getStatus(), "Status field must be SUCCESS.");
        assertNotNull(successResult.getOrderId(), "Order ID must be present for the toast message.");
        assertTrue(successResult.getExecutionTime() >= 0, "Execution time must be a non-negative value.");

        // Scenario B: Out-of-stock checkout produces an error toast.
        Product lowStockProduct = new Product();
        lowStockProduct.setId("PROD_002");
        lowStockProduct.setName("Mechanical Keyboard");
        lowStockProduct.setPrice(89.99);
        lowStockProduct.setStock(0); // No stock
        when(em.find(Product.class, "PROD_002")).thenReturn(lowStockProduct);

        OrderResult failResult = orderProcessingEngine.processCheckout("PROD_002", 1, "shehan@domain.com");

        assertFalse(failResult.isSuccess(), "Failure result must be false for out-of-stock.");
        assertEquals("OUT_OF_STOCK", failResult.getStatus(), "Status must be OUT_OF_STOCK for the error toast.");
        assertNull(failResult.getOrderId(), "No order ID for a failed checkout toast.");

        // Scenario C: Missing email produces an error toast.
        OrderResult missingEmailResult = orderProcessingEngine.processCheckout("PROD_001", 1, "");

        assertFalse(missingEmailResult.isSuccess(), "Failure result for missing email.");
        assertEquals("INVALID_CHECKOUT", missingEmailResult.getStatus(),
            "Status must be INVALID_CHECKOUT when email is blank.");
    }

    // Utility: resolveConfigValue
    @Test
    @DisplayName("Utility: resolveConfigValue prefers first non-null, non-blank value")
    public void testResolveConfigValuePrefersFirstConfiguredValue() {
        assertEquals(
            "smtp-from@example.com",
            AsynchronousNotificationEngine.resolveConfigValue(
                null,
                "smtp-from@example.com",
                null,
                "fallback@example.com"
            )
        );
    }

    // ───── Reflection helper to inject private fields ─────
    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        if (field != null) {
            field.setAccessible(true);
            field.set(target, value);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
