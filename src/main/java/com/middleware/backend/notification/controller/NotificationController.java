//package com.middleware.backend.notification.controller;
//
//import com.middleware.backend.notification.dto.NotificationDto;
//import com.middleware.backend.notification.service.NotificationService;
//import lombok.AllArgsConstructor;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/notifications")
//@AllArgsConstructor
//public class NotificationController {
//    private final NotificationService service;
//
//    @PostMapping("/send")
//    public ResponseEntity<?> send(@RequestBody NotificationDto dto) {
//        return ResponseEntity.ok(service.send(dto));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<?> get(@PathVariable Long id) {
//        return ResponseEntity.ok(service.findById(id));
//    }
//
//    @GetMapping("")
//    public ResponseEntity<?> getAll(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size
//    ) {
//        Pageable pageable = PageRequest.of(page, size);
//        return ResponseEntity.ok(service.findAll(pageable));
//    }
//}
