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
 * Entity representing a notification template.
 *
 * <p>This entity stores template details for notifications, including the name, unique code,
 * type (EMAIL or SMS), subject, body content, activation status, and metadata about creation and updates.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {

    /**
     * Unique identifier for the notification template.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique code for the notification template.
     */
    @Column(name = "code", unique = true)
    private String code;

    /**
     * Name of the template.
     */
    private String name;

    /**
     * Type of the notification channel (EMAIL or SMS).
     */
    @Enumerated(EnumType.STRING)
    private ChannelType type;

    /**
     * Subject of the notification template (used for email notifications).
     */
    private String subject;

    /**
     * Body content of the notification template.
     */
    @Column(columnDefinition = "TEXT")
    private String body;

    /**
     * Indicates whether the template is active.
     */
    private boolean active;

    /**
     * User who created the template.
     */
    @Column(name="created_by")
    private String createdBy;

    /**
     * Timestamp when the template was created.
     */
    @Column(name = "created_at")
    private Timestamp createdAt;

    /**
     * User who last updated the template.
     */
    @Column(name="updated_by")
    private String updatedBy;

    /**
     * Timestamp when the template was last updated.
     */
    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
