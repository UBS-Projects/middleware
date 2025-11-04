package com.middleware.backend.users.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents the user profile information returned by Keycloak's
 * /userinfo endpoint after successful authentication.
 */
@Data
public class KeycloakUser {
    private String sub;
    private String email;
    private String preferred_username;
    private String name;
}