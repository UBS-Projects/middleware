package com.middleware.backend.example;

import org.springframework.stereotype.Service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {



    private final ExternalOrderApiClient externalOrderApiClient;

    public OrderService(ExternalOrderApiClient externalOrderApiClient) {
        this.externalOrderApiClient = externalOrderApiClient;
    }

    /**
     * Process orders and send them to the external API using SpEL-based transformation.
     *
     * @param orders  List of incoming OrderInputItem
     * @param apiName Name of the external API mapping (e.g., "reqbin")
     * @return List of successfully submitted orders
     */
    public List<OrderInputItem> processOrders(List<OrderInputItem> orders, String apiName) {
        return orders.stream()
                .filter(order -> externalOrderApiClient.submitOrder(order, apiName))
                .collect(Collectors.toList());
    }
}
