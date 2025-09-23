package com.middleware.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight option DTO describing a route for selection lists.
 * <p>
 * Contains identifiers and a human-friendly display method used by UIs.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteOptionDto {
    private String routeId;
    private String path;
    private String httpMethod;
    private String description;

    /**
     * Builds a concise label combining the route id and path.
     *
     * @return display text in the form "{routeId} - {path}"
     */
    public String getDisplayText() {
        return routeId + " - " + path;
    }
}