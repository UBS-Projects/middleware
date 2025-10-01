package com.middleware.backend.notification.service;

import com.middleware.backend.notification.dto.ChannelConfigDto;
import com.middleware.backend.notification.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface NotificationService {
    void sendToGroup(List<String> groupCodes, String templateCode, String channelCode);
    ResponseEntity<Page<?>> findAll(Specification<NotificationLog> spec, Pageable pageable);

    ResponseEntity<?> getById(Long id);
}

