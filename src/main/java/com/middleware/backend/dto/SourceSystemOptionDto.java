package com.middleware.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight option DTO for listing/selecting source systems.
 * <p>
 * Provides a simple display text that includes the description when available.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SourceSystemOptionDto {
    private Long id;
    private String name;
    private String description;
    private Boolean active;

    /**
     * Builds a human-readable label combining name and optional description.
     *
     * @return display text like "name - description" when description exists
     */
    public String getDisplayText() {
        return name + (description != null && !description.isEmpty() ? " - " + description : "");
    }
}