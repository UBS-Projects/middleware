package com.middleware.backend.notification.repository;

import com.middleware.backend.notification.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationRecipientGroupRepository
        extends JpaRepository<NotificationRecipientGroup, Long>, JpaSpecificationExecutor<NotificationRecipientGroup> {
}
