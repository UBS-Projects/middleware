package com.middleware.backend.notification.repository;
import com.middleware.backend.notification.model.ChannelConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelConfigRepository extends JpaRepository<ChannelConfig, Long> {
    Page<ChannelConfig> findAll(Specification<ChannelConfig> spec, Pageable pageable);

    Optional<ChannelConfig> findByName(String name);

    void removeById(Long id);

    List<ChannelConfig> findTop5ByNameContainingIgnoreCase(String search);
}