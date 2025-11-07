package com.middleware.component.service;

import com.middleware.component.model.ErrorMappingDetail;

/**
 * Bridge interface for fetching ErrorMapping details from the backend service.
 */
public interface ErrorMappingBridgeService {

    String BEAN_ID = "errorMappingBridgeService";

    /**
     * Fetches an ErrorMappingDetail by ID.
     *
     * @param id The error mapping ID.
     * @return The detailed error mapping info, or null if not found.
     */
    ErrorMappingDetail getErrorMappingById(Long id);
}