package com.middleware.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a request to create or update a receiver.
 *
 * <p>This DTO is used to carry receiver information from the client to the backend
 * service, including metadata and activation status.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiverRequest {

    /**
     * Unique identifier of the receiver.
     * Can be used for updates; may be omitted when creating a new receiver.
     */
    private long id;

    /**
     * Name of the receiver.
     */
    private String name;

    /**
     * Unique code representing the receiver.
     */
    private String code;

    /**
     * Flag indicating whether the receiver is currently active.
     */
    private boolean active;
}

