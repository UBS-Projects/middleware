package com.middleware.backend.dashboard.controller;

import com.middleware.backend.dashboard.service.DashboardService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@AllArgsConstructor
public class DashboardController {
    private final DashboardService service;


    @GetMapping("")
    public ResponseEntity<?> getStatuses(){
        return ResponseEntity.ok(service.getSummary());
    }
    @GetMapping("/transaction/{day}")
    public ResponseEntity<?> getTransactionGraphCoordinates(@PathVariable LocalDate day){
        return ResponseEntity.ok(service.getTransactionGraphCoordinates(day));
    }

    @GetMapping("/job/{day}")
    public ResponseEntity<?> getJobsGraphCoordinates(@PathVariable LocalDate day){
        return ResponseEntity.ok(service.getJobsGraphCoordinates(day));
    }
    @GetMapping("/transaction")
    public ResponseEntity<?> getTransactionList(){
        return ResponseEntity.ok(service.getTransactionList());
    }
    @GetMapping("/job")
    public ResponseEntity<?> getJobsList(){
        return ResponseEntity.ok(service.getJobsList());
    }
    @GetMapping("/token")
    public ResponseEntity<?> getTokenList(){
        return ResponseEntity.ok(service.getTokenList());
    }
}
