package com.middleware.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Data Transfer Object (DTO) representing configuration details.
 * <p>
 * This class encapsulates configuration information including a unique identifier (code)
 * and a map of key-value configuration pairs. It is typically used to transfer configuration
 * data between different layers of the application.
 * </p>
 *
 * @author middleware
 * @version 1.0.0
 * @since 1.0.0
 */
@Getter
@Setter
public class ConfigDetail {

    /**
     * Unique identifier for the configuration group.
     * <p>
     * This code is used to identify and retrieve a specific set of configurations.
     * </p>
     */
    private String code;

    /**
     * Map of configuration key-value pairs.
     * <p>
     * Contains the actual configuration data where the key represents the configuration
     * parameter name and the value represents its corresponding value.
     * </p>
     */
    private Map<String, String> configs;
}