package com.middleware.dhis2.service;

import com.middleware.dhis2.model.Dhis2Request;
import com.middleware.dhis2.model.Dhis2Response;

/**
 * Service interface for DHIS2 operations.
 * This interface will be implemented in the backend project.
 */
public interface Dhis2Service {

    /**
     * Bean ID for Spring registry lookup
     */
    String BEAN_ID = "dhis2ComponentService";

    /**
     * Validates CSV guard parameters and prepares request.
     */
    Dhis2Response validateCsvGuard(Dhis2Request request);

    /**
     * Uploads CSV to DHIS2 with dry-run and commit logic.
     */
    Dhis2Response uploadCsv(Dhis2Request request);

    /**
     * Summarizes DHIS2 import response.
     */
    Dhis2Response summarizeImport(Dhis2Request request);

    /**
     * Complete DHIS2 processing: guard -> upload -> summarize
     */
    Dhis2Response processComplete(Dhis2Request request);
}