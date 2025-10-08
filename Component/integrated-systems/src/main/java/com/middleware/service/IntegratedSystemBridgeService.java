package com.middleware.service;

import com.middleware.model.IntegratedSystemDetail;

public interface IntegratedSystemBridgeService {

    String BEAN_ID = "integratedSystemBridgeService";

    /**
     * Fetch configuration details for a given integrated system.
     *
     * @param code The system code.
     * @return The integrated system details as key-value pairs.
     */
    IntegratedSystemDetail getSystemConfig(String code);
}
