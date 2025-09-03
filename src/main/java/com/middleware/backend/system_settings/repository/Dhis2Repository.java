package com.middleware.backend.system_settings.repository;

import com.middleware.backend.system_settings.dto.Dhis2Dto;
import com.middleware.backend.system_settings.model.Dhis2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Dhis2Repository extends JpaRepository<Dhis2,Long> {
    Optional<Dhis2> findTopByOrderByIdDesc();
}
