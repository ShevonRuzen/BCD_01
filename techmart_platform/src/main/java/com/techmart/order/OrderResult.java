package com.techmart.order;

public class OrderResult {
    private final boolean success;
    private final String orderId;
    private final String status;
    private final double executionTime;

    public OrderResult(boolean success, String orderId, String status, double executionTime) {
        this.success = success;
        this.orderId = orderId;
        this.status = status;
        this.executionTime = executionTime;
    }

    public boolean isSuccess() { return success; }
    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public double getExecutionTime() { return executionTime; }
}