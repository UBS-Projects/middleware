//package com.middleware.backend.users.keycloak.dto;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
//
///**
// * Represents the user profile information returned by Keycloak's
// * /userinfo endpoint after successful authentication.
// */
//@Data
//public class KeycloakUser {
//
//    private String sub; // Unique ID (subject)
//    private String name;
//    private String preferred_username;
//    private String given_name;
//    private String family_name;
//    private String email;
//
//    @JsonProperty("email_verified")
//    private boolean emailVerified;
//}