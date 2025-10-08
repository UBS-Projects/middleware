package com.middleware.backend.integrated_systems.model;

/**
 * Enum representing the types of authentication
 * supported for an integrated system.
 */
public enum AuthenticationType {
    /** No authentication required */
    NONE,

    /** Basic authentication with username and password */
    BASIC,

    /** JWT (JSON Web Token) based authentication */
    JWT
}
