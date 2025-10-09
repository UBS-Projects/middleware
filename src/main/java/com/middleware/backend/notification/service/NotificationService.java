package com.middleware.backend.notification.service;

import com.middleware.backend.notification.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Service interface for sending notifications and retrieving notification logs.
 */
public interface NotificationService {

    /**
     * Sends a notification to multiple groups using a specified template and channel.
     *
     * @param groupCodes   the codes of the groups to send the notification to
     * @param templateCode the code of the notification template to use
     * @param channelCode  the code of the channel (EMAIL or SMS) to use
     * @throws RuntimeException if the template, channel, or any group is not found,
     *                          or if sending fails
     */
    void sendToGroup(List<String> groupCodes, String templateCode, String channelCode);

    /**
     * Retrieves paginated notification logs with optional filtering.
     *
     * @param spec     the specification for filtering logs
     * @param pageable pagination information
     * @return a {@link ResponseEntity} containing a page of notification log DTOs
     */
    ResponseEntity<Page<?>> findAll(Specification<NotificationLog> spec, Pageable pageable);

    /**
     * Retrieves a specific notification log by its ID.
     *
     * @param id the ID of the notification log
     * @return a {@link ResponseEntity} containing the log DTO, or empty if not found
     */
    ResponseEntity<?> getById(Long id);
}

