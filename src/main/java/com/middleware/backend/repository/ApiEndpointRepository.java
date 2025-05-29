package com.middleware.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.middleware.backend.model.ApiEndpoint;

public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, Long> {

    // Object findByPathAndMethod(String path, String method);
    ApiEndpoint findByEndpointPathAndMethod(String endpointPath, String method);

    List<ApiEndpoint> findAll();

    // ApiEndpoint findById();
}
