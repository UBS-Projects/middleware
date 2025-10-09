package com.middleware.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Data Transfer Object representing a token entry displayed on the dashboard.
 * <p>
 * This DTO provides essential information about user tokens, including
 * the associated user, token identifier, and expiry details.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenListDto {

    /**
     * Unique identifier of the token.
     */
    private Long id;

    /**
     * Username associated with the token.
     */
    private String userName;

    /**
     * Unique identifier of the user to whom the token belongs.
     */
    private Long userId;

    /**
     * Expiration date and time of the token.
     */
    private Timestamp expiryDate;
}
