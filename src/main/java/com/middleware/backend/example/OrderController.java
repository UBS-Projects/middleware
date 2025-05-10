package com.middleware.backend.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

//    @PostMapping("/transform")
//    public ResponseEntity<List<OrderInputItem>> transform(@RequestBody OrderRequest request) {
//        List<OrderInputItem> successOrders = orderService.processOrders(request.getOrders());
//        return ResponseEntity.ok(successOrders);
//    }
    @PostMapping("/transform")
    public ResponseEntity<List<OrderInputItem>> transformOrders(
            @RequestParam String api,
            @RequestBody OrderRequest request
    ) {
        List<OrderInputItem> success = orderService.processOrders(request.getOrders(), api);
        return ResponseEntity.ok(success);
    }
}
