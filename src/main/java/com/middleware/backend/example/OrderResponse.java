package com.middleware.backend.example;


import java.util.List;

public class OrderResponse {
    private List<OrderItem> successfulOrders;

    public OrderResponse() {}

    public OrderResponse(List<OrderItem> successfulOrders) {
        this.successfulOrders = successfulOrders;
    }

    public List<OrderItem> getSuccessfulOrders() {
        return successfulOrders;
    }

    public void setSuccessfulOrders(List<OrderItem> successfulOrders) {
        this.successfulOrders = successfulOrders;
    }
}