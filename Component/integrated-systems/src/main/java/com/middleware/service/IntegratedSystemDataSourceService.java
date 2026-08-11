package com.middleware.service;

import com.middleware.model.IntegratedSystemDetail;

import javax.sql.DataSource;

/**
 * Resolves a JDBC {@link DataSource} for DATABASE-type integrated systems.
 * <p>
 * Implemented in the Spring Boot application; looked up from the Camel registry
 * by the {@code integratedSystem} component.
 */
public interface IntegratedSystemDataSourceService {

    String BEAN_ID = "integratedSystemDataSourceService";

    /**
     * Returns a cached {@link DataSource} for the given DATABASE system.
     *
     * @param code   integrated system code
     * @param detail loaded system detail (must include DATABASE config)
     * @return a reusable DataSource (never null for DATABASE systems)
     * @throws IllegalArgumentException if required fields are missing or protocol is unsupported
     */
    DataSource resolveDataSource(String code, IntegratedSystemDetail detail);
}
