package com.middleware.backend.example;


import java.util.List;

public class OrderRequest {
    private List<OrderInputItem> orders;

    public List<OrderInputItem> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderInputItem> orders) {
        this.orders = orders;
    }
}

