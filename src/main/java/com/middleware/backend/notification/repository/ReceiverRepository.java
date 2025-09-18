package com.middleware.backend.notification.repository;
import com.middleware.backend.notification.dto.ReceiverDto;
import com.middleware.backend.notification.model.Receiver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiverRepository extends JpaRepository<Receiver, Long> {
    Page<Receiver> findAll(Specification<Receiver> spec, Pageable pageable);

    Optional<Receiver> findByPhone(String phone);

    Optional<Receiver> findByEmail(String email);

    List<Receiver> findTop5ByNameContainingIgnoreCase(String name);
}