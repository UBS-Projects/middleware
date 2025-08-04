package com.middleware.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteOptionDto {
    private String routeId;
    private String path;
    private String httpMethod;
    private String description;

    public String getDisplayText() {
        return routeId + " - " + path;
    }
}