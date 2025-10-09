package com.middleware.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Data Transfer Object representing a notification channel configuration.
 *
 * <p>This DTO is used for creating, updating, and transferring channel configuration data
 * between the client and the backend service.</p>
 *
 * Fields include channel metadata, configuration details, and audit information.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChannelConfigDto {

    /**
     * Unique identifier of the channel configuration.
     */
    private Long id;

    /**
     * Channel type as a string.
     * Example: "EMAIL" or "SMS".
     */
    private String type;

    /**
     * Human-readable name of the channel.
     * Example: "Default Email".
     */
    private String name;

    /**
     * Unique code representing the channel.
     */
    private String code;

    /**
     * JSON string containing configuration details such as credentials and API URL.
     */
    private String config;

    /**
     * Flag indicating whether the channel is currently active.
     */
    private boolean active;

    /**
     * Username or identifier of the user who created this channel configuration.
     */
    private String createdBy;

    /**
     * Timestamp when the channel configuration was created.
     */
    private Timestamp createdAt;

    /**
     * Username or identifier of the user who last updated this channel configuration.
     */
    private String updatedBy;

    /**
     * Timestamp of the last update to the channel configuration.
     */
    private Timestamp updatedAt;
}
