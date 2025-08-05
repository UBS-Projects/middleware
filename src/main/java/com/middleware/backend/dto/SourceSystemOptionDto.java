package com.middleware.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SourceSystemOptionDto {
    private Long id;
    private String name;
    private String description;
    private Boolean active;

    public String getDisplayText() {
        return name + (description != null && !description.isEmpty() ? " - " + description : "");
    }
}