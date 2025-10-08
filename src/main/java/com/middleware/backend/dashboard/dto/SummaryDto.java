package com.middleware.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Data Transfer Object representing a summarized view of dashboard statistics.
 * <p>
 * This DTO contains key-value pairs summarizing various system metrics,
 * such as total transactions, active jobs, tokens, or other overview indicators.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SummaryDto {

    /**
     * A map containing dashboard summary data where:
     * <ul>
     *     <li><b>Key</b> – the name or label of the metric (e.g., "totalTransactions")</li>
     *     <li><b>Value</b> – the corresponding metric value as a string</li>
     * </ul>
     */
    private Map<String, String> summary;
}
