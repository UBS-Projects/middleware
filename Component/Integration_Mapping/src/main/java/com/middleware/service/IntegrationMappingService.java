package com.middleware.service;


import com.middleware.model.MappingRequest;
import com.middleware.model.MappingResponse;

public interface IntegrationMappingService {
    String BEAN_ID = "integrationMappingComponentService";  // ← الاسم الجديد

    MappingResponse processMapping(MappingRequest request);
}