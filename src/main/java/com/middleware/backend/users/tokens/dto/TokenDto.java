package com.middleware.backend.users.tokens.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Data Transfer Object (DTO) for representing token information.
 * <p>
 * This object is used to transfer token-related data between layers
 * without exposing the full {@link com.middleware.backend.users.tokens.model.Token} entity.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenDto {

    /** Unique identifier of the token */
    private Long id;

    /** Identifier of the user associated with the token */
    private Long userId;

    /** The token string value */
    private String token;

    /** Flag indicating whether the token is still valid */
    private boolean isValid;

    /** Timestamp when the token was created */
    private Timestamp createdAt;

    /** Timestamp when the token will expire */
    private Timestamp expiresAt;
}
