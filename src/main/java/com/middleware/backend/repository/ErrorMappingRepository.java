package com.middleware.backend.repository;

import com.middleware.backend.model.ErrorMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ErrorMappingRepository extends JpaRepository<ErrorMapping, Long> {

   // @Query("SELECT e FROM ErrorMapping e WHERE e.destinationApi.id = :destinationApiId AND e.active = true")
   // List<ErrorMapping> findByDestinationApiId(@Param("destinationApiId") Long destinationApiId);
    List<ErrorMapping> findByDestinationApiId( Long destinationApiId);
    List<ErrorMapping> findByDestinationApiIdAndActive(Long destinationApiId, boolean active);
}
