package com.middleware.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object representing a notification group along with its assigned receivers.
 *
 * <p>This DTO is used to create, update, or transfer group-receiver associations
 * between the client and backend service.</p>
 *
 * Fields include the group ID and a list of receiver IDs assigned to the group.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupReceiversDto {

    /**
     * Unique identifier of the group.
     */
    private Long id;

    /**
     * List of receiver IDs assigned to this group.
     */
    private List<Long> receivers;
}
