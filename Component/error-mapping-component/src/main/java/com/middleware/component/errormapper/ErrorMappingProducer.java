package com.middleware.component.errormapper;

import com.middleware.component.errormapper.model.ErrorMappingDetail;
import com.middleware.component.errormapper.service.ErrorMappingBridgeService;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer that fetches and applies an error mapping based on its ID.
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
        Long code = Long.valueOf(endpoint.getCode());
        if (code == null) {
            code = exchange.getIn().getHeader("errorMappingId", Long.class);
        }

        if (code == null) {
            LOG.warn("No error mapping ID provided (neither in URI nor header 'errorMappingId'). Skipping.");
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            exchange.getIn().setHeader("MappedErrorCode", "MISSING_ID");
            exchange.getIn().setBody("{\"error\":\"Missing error mapping ID\"}");
            return;
        }

        ErrorMappingDetail detail = bridgeService.getErrorMappingById(code);
        if (detail == null) {
            LOG.warn("No error mapping found for ID {}", code);
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            exchange.getIn().setHeader("MappedErrorCode", "NOT_FOUND");
            exchange.getIn().setBody("{\"error\":\"Error mapping not found\"}");
            return;
        }

        LOG.info("Applying error mapping ID={} (code={}, status={})",
                detail.getId(), detail.getMappedErrorCode(), detail.getHttpStatusCode());

        // Apply mapping
        exchange.getIn().setHeader("MappedErrorCode", detail.getMappedErrorCode());
        exchange.getIn().setHeader("MappedMessage", detail.getMappedMessage());
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, detail.getHttpStatusCode());

        exchange.getIn().setBody(detail.getMappedMessage());
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
    }
}