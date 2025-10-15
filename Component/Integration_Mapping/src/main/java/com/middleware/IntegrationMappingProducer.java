package com.middleware;

import com.middleware.model.MappingRequest;
import com.middleware.model.MappingResponse;
import com.middleware.service.IntegrationMappingService;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camel Producer for executing Integration Mapping operations
 */
public class IntegrationMappingProducer extends DefaultProducer {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationMappingProducer.class);

    private final IntegrationMappingEndpoint endpoint;
    private final IntegrationMappingService integrationMappingService;

    public IntegrationMappingProducer(IntegrationMappingEndpoint endpoint,
                                      IntegrationMappingService integrationMappingService) {
        super(endpoint);
        this.endpoint = endpoint;
        this.integrationMappingService = integrationMappingService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // ✅ جيب routeId من Exchange مباشرة
        String routeId = exchange.getFromRouteId();

        // Validation
        if (routeId == null || routeId.isBlank()) {
            MappingResponse error = MappingResponse.error(400, "VALIDATION_ERROR",
                    "Cannot determine route ID from exchange. Make sure the route has an 'id' defined.");
            setResponse(exchange, error);
            return;
        }

        LOG.info("Processing Integration Mapping for routeId: {}", routeId);

        MappingRequest request = buildRequest(exchange, routeId);
        MappingResponse response = integrationMappingService.processMapping(request);

        setResponse(exchange, response);
    }

    private MappingRequest buildRequest(Exchange exchange, String routeId) {
        MappingRequest request = new MappingRequest();

        // ✅ استخدم routeId مباشرة
        request.setMiddlewareApiName(routeId);

        // Camel REST automatically converts query params to headers
        request.setPeriodParam(getHeaderValue(exchange, "pe"));
        request.setOuParam(getHeaderValue(exchange, "ou"));
        request.setDhis2Code(getHeaderValue(exchange, "_dhis2Code"));

        // Support both transaction UUID header names
        String transactionUUID = getHeaderValue(exchange, "transactionUUID");
        if (transactionUUID == null) {
            transactionUUID = getHeaderValue(exchange, "X-Transaction-UUID");
        }
        request.setTransactionUUID(transactionUUID);

        LOG.info("Built request - RouteID: {}, pe: {}, ou: {}, dhis2Code: {}, UUID: {}",
                routeId, request.getPeriodParam(), request.getOuParam(),
                request.getDhis2Code(), transactionUUID);

        return request;
    }

    private void setResponse(Exchange exchange, MappingResponse response) {
        if (!response.isSuccess()) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, response.getHttpStatusCode());
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");

            String jsonResponse = String.format(
                    "{\"status\":\"%s\",\"message\":\"%s\"}",
                    escape(response.getStatus()),
                    escape(response.getMessage())
            );

            exchange.getIn().setBody(jsonResponse);
            exchange.setRouteStop(true);
            return;
        }

        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, response.getHttpStatusCode());
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getIn().setBody(response.getData());

        exchange.setProperty("mapping.status", response.getStatus());
        exchange.setProperty("mapping.success", response.isSuccess());
    }

    private String getHeaderValue(Exchange exchange, String headerName) {
        return exchange.getIn().getHeader(headerName, String.class);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}