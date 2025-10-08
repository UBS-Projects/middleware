package com.middleware.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a transaction record displayed on the dashboard.
 * <p>
 * This DTO provides details about API transactions, including endpoint,
 * response status, execution duration, and timestamp of occurrence.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionListDto {

    /**
     * Unique identifier of the transaction.
     */
    private String id;

    /**
     * API endpoint that was called during the transaction.
     */
    private String endPoint;

    /**
     * HTTP response code returned from the transaction.
     */
    private Integer responseCode;

    /**
     * Timestamp representing when the transaction event occurred.
     */
    private LocalDateTime eventTime;

    /**
     * Duration of the transaction execution in milliseconds.
     */
    private long duration;
}
