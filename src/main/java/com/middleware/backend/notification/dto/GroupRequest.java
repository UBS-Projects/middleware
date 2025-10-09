package com.middleware.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * Data Transfer Object representing a request to create or update a notification group.
 *
 * <p>This DTO carries basic information about a notification group, such as its ID,
 * name, and unique code, between the client and backend service.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupRequest {

    /**
     * Unique identifier of the group.
     * Can be null when creating a new group.
     */
    private Long id;

    /**
     * Human-readable name of the group.
     */
    private String groupName;

    /**
     * Unique code representing the group.
     */
    private String code;
}
