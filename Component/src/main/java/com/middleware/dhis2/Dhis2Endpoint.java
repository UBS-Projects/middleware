package com.middleware.dhis2;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middleware.dhis2.service.Dhis2Service;

import lombok.Getter;
import lombok.Setter;

/**
 * DHIS2 Integration Endpoint for CSV uploads and data synchronization
 */
@Getter
@Setter
@UriEndpoint(firstVersion = "1.0.0", scheme = "hakeem", title = "hakeem  ", syntax = "hakeem:operation", category = {
        Category.MESSAGING, Category.TRANSFORMATION }, producerOnly = true)

public class Dhis2Endpoint extends DefaultEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(Dhis2Endpoint.class);

    @UriPath
    @Metadata(required = true, description = "The operation to perform. Options: csvUpload, guard, summarize")
    private String operation;

    @UriParam(defaultValue = "true", description = "Whether to perform dry-run before actual import")
    private boolean dryRun = true;

    @UriParam(defaultValue = "CODE", description = "ID scheme to use (CODE or UID)")
    private String idScheme = "CODE";

    @UriParam(defaultValue = "NEW_AND_UPDATES", description = "Import strategy (NEW, UPDATES, NEW_AND_UPDATES, DELETE)")
    private String strategy = "NEW_AND_UPDATES";

    @UriParam(defaultValue = "30000", description = "Connection timeout in milliseconds")
    private int connectionTimeout = 30000;

    @UriParam(defaultValue = "60000", description = "Read timeout in milliseconds")
    private int readTimeout = 60000;

    // The Dhis2Service is passed from the component
    private Dhis2Service dhis2Service;

    public Dhis2Endpoint(String uri, Dhis2Component component, String operation) {
        super(uri, component);
        this.operation = operation;
        LOG.info("Dhis2Endpoint created for URI: {} with operation: {}", uri, operation);
    }

    @Override
    public Producer createProducer() throws Exception {
        LOG.info("Creating Dhis2Producer for endpoint: {} with operation: {}", getEndpointUri(), operation);
        return new Dhis2Producer(this, dhis2Service);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException(
                "DHIS2 component is producer only and not intended for consuming messages directly.");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}