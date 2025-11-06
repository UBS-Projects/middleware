package com.middleware.component;

import com.middleware.component.model.ErrorMappingDetail;
import com.middleware.component.service.ErrorMappingBridgeService;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer that fetches and applies an error mapping based on its ID.
 * Applies mapping only when the current HTTP response code indicates failure (>= 400).
 */
public class ErrorMappingProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorMappingProducer.class);

    private final ErrorMappingEndpoint endpoint;
    private final ErrorMappingBridgeService bridgeService;

    public ErrorMappingProducer(ErrorMappingEndpoint endpoint, ErrorMappingBridgeService bridgeService) {
        super(endpoint);
        this.endpoint = endpoint;
        this.bridgeService = bridgeService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // Try to extract mapping ID
        Long code = null;
        try {
            if (endpoint.getCode() != null && !endpoint.getCode().isEmpty()) {
                code = Long.valueOf(endpoint.getCode());
            }
        } catch (NumberFormatException ignored) {}

        if (code == null) {
            code = exchange.getIn().getHeader("errorMappingId", Long.class);
        }

        if (code == null) {
            LOG.debug("No error mapping ID provided (neither in URI nor header 'errorMappingId'). Skipping mapping.");
            return;
        }

        // Get current response code (default to 200 if none)
        Integer httpCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (httpCode == null) httpCode = 200;

        // ✅ Only apply mapping if HTTP status indicates failure
        if (httpCode < 400) {
            LOG.debug("HTTP {} indicates success — skipping error mapping (ID={}).", httpCode, code);
            return;
        }

        // Fetch the mapping definition
        ErrorMappingDetail detail = bridgeService.getErrorMappingById(code);
        if (detail == null) {
            LOG.warn("No error mapping found for ID {}", code);
            exchange.getIn().setHeader("MappedErrorCode", "NOT_FOUND");
            exchange.getIn().setHeader("MappedMessage", "Error mapping not found");
            return;
        }

        // ✅ Apply mapping
        LOG.info("Applying error mapping ID={} (mappedCode={}, status={})",
                detail.getId(), detail.getMappedErrorCode(), detail.getHttpStatusCode());

        exchange.getIn().setHeader("MappedErrorCode", detail.getMappedErrorCode());
        exchange.getIn().setHeader("MappedMessage", detail.getMappedMessage());
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, detail.getHttpStatusCode());
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");

        // Set response body to JSON with mapped info
        String body = String.format(
                "{\"error\":\"%s\",\"code\":\"%s\"}",
                detail.getMappedMessage(), detail.getMappedErrorCode()
        );
        exchange.getIn().setBody(body);
    }
}
