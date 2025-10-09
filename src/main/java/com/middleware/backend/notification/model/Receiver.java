package com.middleware.backend.notification.model;
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
 * Entity representing a notification receiver.
 *
 * <p>This entity stores information about individual receivers, including their
 * name, email, phone number, and metadata about who created or updated the record and when.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notification_receivers")
public class Receiver {

    /**
     * Unique identifier for the receiver.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Full name of the receiver.
     */
    private String name;

    /**
     * Email address of the receiver.
     */
    private String email;

    /**
     * Phone number of the receiver.
     */
    private String phone;

    /**
     * User who created this receiver record.
     */
    @Column(name="created_by")
    private String createdBy;

    /**
     * Timestamp when this receiver record was created.
     */
    @Column(name = "created_at")
    private Timestamp createdAt;

    /**
     * User who last updated this receiver record.
     */
    @Column(name="updated_by")
    private String updatedBy;

    /**
     * Timestamp when this receiver record was last updated.
     */
    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
