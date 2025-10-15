package com.middleware.service;

import com.middleware.model.IntegratedSystemDetail;

/**
 * Service interface for fetching configuration details
 * for integrated systems by their code.
 */
public interface IntegratedSystemBridgeService {

    /**
     * The default bean ID for the integrated system bridge service.
     */
    String BEAN_ID = "integratedSystemBridgeService";

    /**
     * Fetch configuration details for a given integrated system.
     *
     * @param code The system code.
     * @return The integrated system details as key-value pairs.
     */
    IntegratedSystemDetail getSystemConfig(String code);
}