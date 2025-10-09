package com.middleware.backend.notification.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Data Transfer Object representing a receiver of notifications.
 *
 * <p>This DTO is used to transfer receiver information between the client and
 * backend service, including contact details and audit information.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiverDto {

    /**
     * Unique identifier of the receiver.
     */
    private Long id;

    /**
     * Name of the receiver.
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
     * Username or identifier of the user who created the receiver record.
     */
    private String createdBy;

    /**
     * Timestamp when the receiver record was created.
     */
    private Timestamp createdAt;

    /**
     * Username or identifier of the user who last updated the receiver record.
     */
    private String updatedBy;

    /**
     * Timestamp when the receiver record was last updated.
     */
    private Timestamp updatedAt;
}
