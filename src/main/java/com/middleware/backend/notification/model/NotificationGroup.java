package com.middleware.backend.notification.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;
import java.util.List;

/**
 * Entity representing a notification group.
 *
 * <p>This entity stores information about a group of receivers for notifications,
 * including the group's name, unique code, description, assigned receivers, and metadata
 * about creation and updates.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notification_groups")
public class NotificationGroup {

    /**
     * Unique identifier for the notification group.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name of the notification group.
     */
    private String name;

    /**
     * Unique code for the notification group.
     */
    @Column(name = "code", unique = true)
    private String code;

    /**
     * Description of the group.
     */
    private String description;

    /**
     * List of receivers associated with this group.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Receiver> receivers;

    /**
     * Indicates whether the group is active.
     */
    private boolean active;

    /**
     * User who created the group.
     */
    @Column(name="created_by")
    private String createdBy;

    /**
     * Timestamp when the group was created.
     */
    @Column(name = "created_at")
    private Timestamp createdAt;

    /**
     * User who last updated the group.
     */
    @Column(name="updated_by")
    private String updatedBy;

    /**
     * Timestamp when the group was last updated.
     */
    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
