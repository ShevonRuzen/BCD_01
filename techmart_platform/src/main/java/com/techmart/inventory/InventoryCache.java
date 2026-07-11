package com.techmart.inventory;

import com.techmart.model.Product;
import com.techmart.monitoring.PerformanceMetricsCollector;
import com.techmart.service.ProductCatalogService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class InventoryCache {

    private static final Logger LOGGER = Logger.getLogger(InventoryCache.class.getName());

    @EJB
    private PerformanceMetricsCollector metricsCollector;

    @EJB
    private ProductCatalogService productCatalogService;

    private Map<String, Integer> stockLevels;

    @PostConstruct
    public void init() {
        LOGGER.info("Singleton InventoryCache @PostConstruct initialization.");
        stockLevels = new HashMap<>();
        // Seed cache from known product records
        syncFromDatabase("PROD_001");
        syncFromDatabase("PROD_002");
    }

    @Lock(LockType.READ)
    public int getStock(String productId) {
        if (productId == null || productId.isBlank()) {
            return 0;
        }
        if (!stockLevels.containsKey(productId)) {
            syncFromDatabase(productId);
        }
        return stockLevels.getOrDefault(productId, 0);
    }
    @Lock(LockType.WRITE)
    public boolean decrementStock(String productId, int quantity) {
        if (productId == null || productId.isBlank() || quantity <= 0) {
            return false;
        }
        if (!stockLevels.containsKey(productId)) {
            syncFromDatabase(productId);
        }
        int currentStock = stockLevels.getOrDefault(productId, 0);
        boolean success = (currentStock >= quantity);
        if (success) {
            stockLevels.put(productId, currentStock - quantity);
        }
        if (metricsCollector != null) {
            metricsCollector.recordCacheHit(success);
        }
        return success;
    }
    @Lock(LockType.WRITE)
    public void setStock(String productId, int stock) {
        if (productId == null || productId.isBlank() || stock < 0) {
            return;
        }
        stockLevels.put(productId, stock);
    }

    private void syncFromDatabase(String productId) {
        if (productCatalogService == null) {
            return;
        }
        Product product = productCatalogService.getProductById(productId);
        if (product != null) {
            stockLevels.put(productId, product.getStock());
        }
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.info("Singleton InventoryCache @PreDestroy cleanup.");
    }
}