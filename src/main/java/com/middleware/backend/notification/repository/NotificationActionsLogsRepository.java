package com.middleware.backend.notification.repository;

import com.middleware.backend.notification.model.ChannelConfig;
import com.middleware.backend.notification.model.NotificationActionsLogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationActionsLogsRepository extends JpaRepository<NotificationActionsLogs,Long> {
    Page<NotificationActionsLogs> findAll(Specification<NotificationActionsLogs> spec, Pageable pageable);
}
