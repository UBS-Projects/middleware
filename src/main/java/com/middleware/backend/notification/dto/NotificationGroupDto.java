package com.middleware.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

/**
 * Data Transfer Object representing a notification group.
 *
 * <p>This DTO is used to transfer information about notification groups,
 * including metadata, associated receivers, and audit information.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationGroupDto {

    /**
     * Unique identifier of the notification group.
     */
    private Long id;

    /**
     * Human-readable name of the notification group.
     */
    private String name;

    /**
     * Unique code representing the notification group.
     */
    private String code;

    /**
     * Description of the notification group.
     */
    private String description;

    /**
     * List of receivers associated with this group.
     */
    private List<ReceiverDto> receivers;

    /**
     * Flag indicating whether the group is currently active.
     */
    private boolean active;

    /**
     * Username or identifier of the user who created the group.
     */
    private String createdBy;

    /**
     * Timestamp when the group was created.
     */
    private Timestamp createdAt;

    /**
     * Username or identifier of the user who last updated the group.
     */
    private String updatedBy;

    /**
     * Timestamp when the group was last updated.
     */
    private Timestamp updatedAt;
}

