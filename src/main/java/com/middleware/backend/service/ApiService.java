package com.middleware.backend.service;

import com.middleware.backend.model.ApiEndpoint;
import com.middleware.backend.repository.ApiEndpointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiService {

    @Autowired
    private ApiEndpointRepository apiEndpointRepository;

    public List<ApiEndpoint> getAllEndpoints() {
        return apiEndpointRepository.findAll();
    }

    public ApiEndpoint save(ApiEndpoint endpoint) {
        return apiEndpointRepository.save(endpoint);
    }
}
