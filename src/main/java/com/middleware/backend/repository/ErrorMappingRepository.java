package com.middleware.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.middleware.backend.model.ErrorMapping;

public interface ErrorMappingRepository extends JpaRepository<ErrorMapping, Long>, JpaSpecificationExecutor<ErrorMapping> {

    List<ErrorMapping> findByDestinationApiId(Long destinationApiId);

    List<ErrorMapping> findByDestinationApiIdAndActive(Long destinationApiId, boolean active);
}
