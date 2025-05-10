package com.middleware.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.middleware.backend.model.DestinationApi;

@Repository
public interface DestinationApiRepository extends JpaRepository<DestinationApi, Long> {
}
