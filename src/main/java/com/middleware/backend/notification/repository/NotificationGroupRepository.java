package com.middleware.backend.notification.repository;

import com.middleware.backend.notification.dto.NotificationGroupDto;
import com.middleware.backend.notification.dto.ReceiverRequest;
import com.middleware.backend.notification.model.NotificationGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationGroupRepository extends JpaRepository<NotificationGroup,Long> {
    Page<NotificationGroup> findAll(Specification<NotificationGroup> spec, Pageable pageable);

    Optional<NotificationGroup> findByName(String name);

    @Query("SELECT g FROM NotificationGroup g WHERE g.receivers IS NOT EMPTY")
    Page<NotificationGroup> findAllWithReceivers(Pageable pageable);

    @Query("SELECT g FROM NotificationGroup g WHERE g.receivers IS EMPTY")
    List<NotificationGroup> findAllWithoutReceivers();

    List<NotificationGroup> findTop5ByNameContainingIgnoreCase(String search);

    Optional<NotificationGroup> findByCode(String trim);
}
