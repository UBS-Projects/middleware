package com.middleware.component.errormapper.service;

import com.middleware.component.errormapper.model.ErrorMappingDetail;

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