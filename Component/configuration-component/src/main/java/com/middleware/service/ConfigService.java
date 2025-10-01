package com.middleware.service;


import com.middleware.model.ConfigDetail;

public interface ConfigService {

    String BEAN_ID = "configService";

    /**
     * Retrieves configuration details for a given code.
     *
     * @param code The code identifying the configuration group.
     * @return The configuration detail.
     */
    ConfigDetail getConfigs(String code);
}