package com.middleware.backend.integrated_systems.dto;

import com.middleware.backend.integrated_systems.model.AuthenticationType;
import com.middleware.backend.integrated_systems.model.Protocol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * DTO for IntegratedSystem entity.
 * Used to transfer system data without exposing entity directly.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IntegratedSystemDto {

    private Long id;

    private String code;

    private String host;

    private String port;

    private String description;

    private Protocol protocol;

    private String additionalAttribute1;

    private String additionalAttribute2;

    private AuthenticationType authenticationType;

    // Only used if authenticationType == BASIC
    private String username;
    private String password;

    // Only used if authenticationType == JWT
    private String token;


    private String createdBy;
    private Timestamp createdAt;
    private String updatedBy;
    private Timestamp updatedAt;

}