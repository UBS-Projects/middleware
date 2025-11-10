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
     * @param code The error mapping CODE.
     * @return The detailed error mapping info, or null if not found.
     */
    ErrorMappingDetail getErrorMappingByCode(String code);
}