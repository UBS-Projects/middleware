package com.middleware.backend.dashboard.controller;

import com.middleware.backend.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controller responsible for exposing dashboard-related endpoints such as summaries,
 * transaction and job graphs, and entity lists.
 */
@RestController
@RequestMapping("/api/dashboard")
@AllArgsConstructor
public class DashboardController {

    private final DashboardService service;

    /**
     * Retrieves a summarized overview of system statistics for the dashboard.
     *
     * @return 200 with summary data including total counts and statuses
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('dashboard:summary')")
    @Operation(
            summary = "Get dashboard summary",
            description = "Retrieves a summarized overview of the dashboard including counts, system status, and general metrics."
    )
    public ResponseEntity<?> getStatuses() {
        return ResponseEntity.ok(service.getSummary());
    }

    /**
     * Retrieves transaction graph data points for a specific day.
     *
     * @param day the date for which to retrieve transaction graph data (format: YYYY-MM-DD)
     * @return 200 with graph coordinate data representing transaction activity
     */
    @GetMapping("/transaction/last-30-days")
    @PreAuthorize("hasAuthority('dashboard:transactionGraph')")
    @Operation(
            summary = "Get transaction graph data",
            description = "Retrieves coordinate points representing transaction activity on a specific day."
    )
    public ResponseEntity<?> getTransactionGraphCoordinates() {
        return ResponseEntity.ok(service.getTransactionGraphCoordinatesLast30Days());
    }

    /**
     * Retrieves job graph data points for a specific day.
     *
     * @param day the date for which to retrieve job execution graph data (format: YYYY-MM-DD)
     * @return 200 with graph coordinate data representing job activity
     */
    @GetMapping("/job/last-30-days")
    @PreAuthorize("hasAuthority('dashboard:jobGraph')")
    @Operation(
            summary = "Get job graph data",
            description = "Retrieves coordinate points representing scheduled job executions for a specific day."
    )
    public ResponseEntity<?> getJobsGraphCoordinates() {
        return ResponseEntity.ok(service.getJobsGraphCoordinatesLast30Days());
    }

    /**
     * Retrieves a list of recent transactions for display on the dashboard.
     *
     * @return 200 with a list of recent transactions
     */
    @GetMapping("/transaction")
    @PreAuthorize("hasAuthority('dashboard:transactionList')")
    @Operation(
            summary = "Get transaction list",
            description = "Fetches a list of recent transactions displayed in the dashboard transaction table."
    )
    public ResponseEntity<?> getTransactionList() {
        return ResponseEntity.ok(service.getTransactionList());
    }

    /**
     * Retrieves a list of recent jobs for display on the dashboard.
     *
     * @return 200 with a list of recent scheduled jobs
     */
    @GetMapping("/job")
    @PreAuthorize("hasAuthority('dashboard:jobList')")
    @Operation(
            summary = "Get job list",
            description = "Fetches a list of recently executed or scheduled jobs displayed in the dashboard job table."
    )
    public ResponseEntity<?> getJobsList() {
        return ResponseEntity.ok(service.getJobsList());
    }

    /**
     * Retrieves a list of active tokens displayed on the dashboard.
     *
     * @return 200 with a list of tokens including their status and related metadata
     */
    @GetMapping("/token")
    @PreAuthorize("hasAuthority('dashboard:tokenList')")
    @Operation(
            summary = "Get token list",
            description = "Retrieves a list of system tokens including their creation date, status, and related user or system information."
    )
    public ResponseEntity<?> getTokenList() {
        return ResponseEntity.ok(service.getTokenList());
    }
}
