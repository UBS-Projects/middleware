package com.middleware.backend.system_settings.model;

/**
 * Enum representing the type of a configuration entry.
 * Used to define how the config value should be interpreted or validated.
 */
public enum ConfigType {
    /**
     * Represents a plain text string value.
     */
    STRING,

    /**
     * Represents a numeric value.
     */
    NUMBER,

    /**
     * Represents a boolean value (true/false).
     */
    BOOLEAN,

    /**
     * Represents a JSON object or array as the config value.
     */
    JSON
}
