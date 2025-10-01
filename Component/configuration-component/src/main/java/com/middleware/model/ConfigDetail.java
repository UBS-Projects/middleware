package com.middleware.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO representing configuration details.
 */
@Getter
@Setter
public class ConfigDetail {
    private String code;         // identifier for config group
    private Map<String, String> configs; // key-value map of configs
}