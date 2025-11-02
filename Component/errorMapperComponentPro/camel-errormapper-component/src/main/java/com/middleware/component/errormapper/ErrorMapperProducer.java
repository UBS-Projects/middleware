package com.middleware.component.errormapper;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middleware.component.errormapper.model.ErrorMappingDetail;
import com.middleware.component.errormapper.service.ErrorMappingService;

/**
 * The producer for the ErrorMapper component.
 * It consumes messages and applies the configured error mapping,
 * transforming HTTP status codes and response bodies as needed.
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
        // Extract HTTP status and exception info
        Integer httpCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        String httpText = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_TEXT, String.class);
        String errorMessage = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, String.class);

        // If no exception, use HTTP error text as error message
        if (errorMessage == null && httpCode != null && httpCode >= 400) {
            errorMessage = httpText;
        }

        String routeId = exchange.getFromRouteId();
        Long sourceSystemId = exchange.getProperty("sourceSystemId", Long.class);

        // Find matching error mapping
        ErrorMappingDetail matchingError = errorMappingService.findMatchingErrorMapping(
                routeId,
                sourceSystemId,
                errorMessage
        );


        if (matchingError != null) {
            LOG.info("Applying error mapping for routeId: '{}'", matchingError.getRouteId());

            // Set mapped headers
            exchange.getIn().setHeader("MappedErrorCode", matchingError.getMappedErrorCode());
            exchange.getIn().setHeader("MappedMessage", matchingError.getMappedMessage());

            // Override HTTP status code
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, matchingError.getHttpStatusCode());

            // Set response body (can be plain text or JSON)
            exchange.getIn().setBody(matchingError.getMappedMessage());

            // Optional: set content type for JSON response
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");

        } else {
            LOG.warn("No matching error mapping found for routeId: '{}', sourceSystemId: '{}', errorMessage: '{}'",
                    routeId, sourceSystemId, errorMessage);

            // Default error handling
            exchange.getIn().setHeader("MappedErrorCode", "DEFAULT_ERROR");
            exchange.getIn().setHeader("MappedMessage", "An unexpected error occurred.");
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
            exchange.getIn().setBody("{\"error\":\"An unexpected error occurred.\"}");
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        }
    }
}
