package com.techmart.monitoring;

import com.techmart.inventory.InventoryCache;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import javax.naming.InitialContext;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class PerformanceMetricsCollector {

    @EJB
    private InventoryCache inventoryCache;

    private final AtomicLong totalCheckouts = new AtomicLong(0);
    private final AtomicLong successfulCheckouts = new AtomicLong(0);
    private final AtomicLong failedCheckouts = new AtomicLong(0);
    
    private final AtomicInteger activeSessions = new AtomicInteger(0);
    
    private final AtomicLong totalCheckoutTimeMs = new AtomicLong(0);
    private final AtomicLong totalDbPersistenceTimeMs = new AtomicLong(0);
    private final AtomicLong dbPersistCount = new AtomicLong(0);
    
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    @Lock(LockType.WRITE)
    public void recordCheckout(boolean success, double durationMs) {
        totalCheckouts.incrementAndGet();
        if (success) {
            successfulCheckouts.incrementAndGet();
        } else {
            failedCheckouts.incrementAndGet();
        }
        totalCheckoutTimeMs.addAndGet((long) (durationMs * 1000)); // Store microsecond precision
    }

    @Lock(LockType.WRITE)
    public void recordDbPersistence(double durationMs) {
        dbPersistCount.incrementAndGet();
        totalDbPersistenceTimeMs.addAndGet((long) (durationMs * 1000));
    }

    @Lock(LockType.WRITE)
    public void recordCacheHit(boolean hit) {
        if (hit) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
    }

    @Lock(LockType.WRITE)
    public void incrementActiveSessions() {
        activeSessions.incrementAndGet();
    }

    @Lock(LockType.WRITE)
    public void decrementActiveSessions() {
        activeSessions.decrementAndGet();
        if (activeSessions.get() < 0) {
            activeSessions.set(0);
        }
    }

    @Lock(LockType.READ)
    public long getTotalCheckouts() { return totalCheckouts.get(); }

    @Lock(LockType.READ)
    public long getSuccessfulCheckouts() { return successfulCheckouts.get(); }

    @Lock(LockType.READ)
    public long getFailedCheckouts() { return failedCheckouts.get(); }

    @Lock(LockType.READ)
    public int getActiveSessions() { return activeSessions.get(); }

    @Lock(LockType.READ)
    public double getAverageCheckoutTimeMs() {
        long count = totalCheckouts.get();
        if (count == 0) return 0.0;
        return (totalCheckoutTimeMs.get() / 1000.0) / count;
    }

    @Lock(LockType.READ)
    public double getAverageDbPersistenceTimeMs() {
        long count = dbPersistCount.get();
        if (count == 0) return 0.0;
        return (totalDbPersistenceTimeMs.get() / 1000.0) / count;
    }

    @Lock(LockType.READ)
    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        if (total == 0) return 100.0;
        return ((double) hits / total) * 100.0;
    }

    /**
     * Compares 1000 JNDI lookups vs CDI Injection times.
     * @return double[]{cdiDurationMs, jndiDurationMs}
     */
    public double[] runJndiVsCdiBenchmark() {
        int iterations = 1000;
        
        // 1. CDI Injection
        long startCdi = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            inventoryCache.getStock("PROD_001");
        }
        long endCdi = System.nanoTime();
        double cdiDurationMs = (endCdi - startCdi) / 1_000_000.0;

        // 2. JNDI Lookup
        long startJndi = System.nanoTime();
        try {
            InitialContext ctx = new InitialContext();
            for (int i = 0; i < iterations; i++) {
                InventoryCache cache = (InventoryCache) ctx.lookup("java:module/InventoryCache");
                cache.getStock("PROD_001");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        long endJndi = System.nanoTime();
        double jndiDurationMs = (endJndi - startJndi) / 1_000_000.0;

        return new double[]{cdiDurationMs, jndiDurationMs};
    }
}
