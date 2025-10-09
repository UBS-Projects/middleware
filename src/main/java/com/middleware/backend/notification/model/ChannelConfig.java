package com.middleware.backend.notification.model;
import com.middleware.backend.notification.enums.ChannelType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

/**
 * Entity representing a notification channel configuration.
 *
 * <p>This entity stores the configuration details for a notification channel,
 * such as email or SMS, including credentials, API settings, and metadata about creation and updates.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notification_channels")
public class ChannelConfig {

    /**
     * Unique identifier for the channel configuration.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the channel, e.g., "Default Email".
     */
    private String name;

    /**
     * Unique code for the channel configuration.
     */
    @Column(name = "code", unique = true)
    private String code;

    /**
     * Type of the channel (EMAIL or SMS).
     */
    @Enumerated(EnumType.STRING)
    private ChannelType type;

    /**
     * Configuration details in JSON format, such as credentials and API URL.
     */
    @Column(columnDefinition = "TEXT")
    private String config;

    /**
     * Whether the channel is active.
     */
    private boolean active;

    /**
     * User who created this channel configuration.
     */
    @Column(name="created_by")
    private String createdBy;

    /**
     * Timestamp of when the channel configuration was created.
     */
    @Column(name = "created_at")
    private Timestamp createdAt;

    /**
     * User who last updated this channel configuration.
     */
    @Column(name="updated_by")
    private String updatedBy;

    /**
     * Timestamp of the last update of this channel configuration.
     */
    @Column(name = "updated_at")
    private Timestamp updatedAt;
}

