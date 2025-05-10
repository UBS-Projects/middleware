package com.middleware.backend.example;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalApiRepository extends JpaRepository<ExternalApi, Long> {
    Optional<ExternalApi> findByName(String name);
}

