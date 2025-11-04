package com.middleware.backend.users.keycloak.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents the token response returned by Keycloak after exchanging
 * an authorization code or performing a token refresh.
 */
@Data
public class KeycloakTokenResponse {
    private String access_token;
    private String refresh_token;
    private Long expires_in;
    private String id_token;
}