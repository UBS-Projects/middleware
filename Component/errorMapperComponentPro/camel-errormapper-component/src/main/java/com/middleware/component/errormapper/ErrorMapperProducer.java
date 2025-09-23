package com.middleware.component.errormapper;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middleware.component.errormapper.model.ErrorMappingDetail;
import com.middleware.component.errormapper.service.ErrorMappingService;

/**
 * The producer for the ErrorMapper component. It consumes messages and applies
 * the configured error mapping.
 */
public class ErrorMapperProducer extends DefaultProducer {
    private static final Logger LOG = LoggerFactory.getLogger(ErrorMapperProducer.class);

    private final ErrorMapperEndpoint endpoint;
    private final ErrorMappingService errorMappingService;

    public ErrorMapperProducer(ErrorMapperEndpoint endpoint, ErrorMappingService errorMappingService) {
        super(endpoint);
        this.endpoint = endpoint;
        this.errorMappingService = errorMappingService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // Retrieve routeId, sourceSystemId, and errorMessage from the exchange
        // These can be set as headers or properties on the exchange.
        String routeId = exchange.getProperty("routeId", String.class);
        Long sourceSystemId = exchange.getProperty("sourceSystemId", Long.class);
        String errorMessage = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, String.class);

        // This is the key change. We now call the service to find the single matching
        // error.
        ErrorMappingDetail matchingError = errorMappingService.findMatchingErrorMapping(routeId, sourceSystemId,
                errorMessage);

        if (matchingError != null) {
            LOG.info("Applying error mapping for routeId: '{}'", matchingError.getRouteId());

            // Set the mapped error details back onto the exchange
            exchange.getIn().setHeader("MappedErrorCode", matchingError.getMappedErrorCode());
            exchange.getIn().setHeader("MappedMessage", matchingError.getMappedMessage());
            exchange.getIn().setHeader("HttpStatusCode", matchingError.getHttpStatusCode());
            // You can set other details as needed
        } else {
            LOG.warn("No matching error mapping found for routeId: '{}', sourceSystemId: '{}', errorMessage: '{}'",
                    routeId, sourceSystemId, errorMessage);
            // Optionally, handle the case where no match is found, e.g., set a default
            // error
            exchange.getIn().setHeader("MappedErrorCode", "DEFAULT_ERROR");
            exchange.getIn().setHeader("MappedMessage", "An unexpected error occurred.");
            exchange.getIn().setHeader("HttpStatusCode", 500);
        }
    }
}
