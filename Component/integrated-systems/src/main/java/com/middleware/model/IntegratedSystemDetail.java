package com.middleware.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents the configuration details for an integrated system,
 * including its unique code and a configuration map.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IntegratedSystemDetail {

    /**
     * Unique code identifying the integrated system.
     */
    private String code;

    /**
     * Configuration map containing key-value pairs for the system.
     */
    private Map<String, Object> config;
}