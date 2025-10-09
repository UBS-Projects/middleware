package com.middleware.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a receiver within a notification group.
 *
 * <p>This DTO is used to transfer detailed information about group-receiver associations,
 * including group and receiver names.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupReceiversDto2 {

    /**
     * Unique identifier of the group.
     */
    private long groupId;

    /**
     * Human-readable name of the group.
     */
    private String groupName;

    /**
     * Name of the receiver assigned to the group.
     */
    private String receiverName;
}
