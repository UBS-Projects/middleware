package com.middleware.backend.users.model;

/**
 * Enumeration representing different modes for string matching operations.
 * <p>
 * This enum can be used in search or filtering logic where
 * flexible pattern matching is required.
 * </p>
 */
public enum MatchMode {
        /** Matches the string exactly */
        EXACT,

        /** Matches strings that start with the given pattern */
        STARTS_WITH,

        /** Matches strings that end with the given pattern */
        ENDS_WITH,

        /** Matches strings that contain the given pattern */
        CONTAINS
}
