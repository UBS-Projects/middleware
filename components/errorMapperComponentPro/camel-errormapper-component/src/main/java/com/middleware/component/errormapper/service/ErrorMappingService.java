package com.middleware.component.errormapper.service;

import com.middleware.component.errormapper.model.ErrorMappingDetail;

/**
 * Interface for providing error mapping rules to the ErrorMapper component.
 * This can be implemented to retrieve mappings from a file, a database, or any
 * other source.
 */
public interface ErrorMappingService {

    String BEAN_ID = "errorMappingService";

    /**
     * Finds a single matching error mapping based on route ID, source system ID,
     * and error message.
     *
     * @param routeId        The ID of the route where the error occurred.
     * @param sourceSystemId The ID of the source system.
     * @param errorMessage   The raw error message from the exception.
     * @return The detailed error mapping if a match is found, otherwise null.
     */
    ErrorMappingDetail findMatchingErrorMapping(String routeId, Long sourceSystemId, String errorMessage);
}
