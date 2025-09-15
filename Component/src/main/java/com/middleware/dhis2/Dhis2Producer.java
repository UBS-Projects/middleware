package com.middleware.dhis2;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middleware.dhis2.model.Dhis2Request;
import com.middleware.dhis2.model.Dhis2Response;
import com.middleware.dhis2.service.Dhis2Service;

/**
 * Producer for DHIS2 Component operations
 */
public class Dhis2Producer extends DefaultProducer {
    private static final Logger LOG = LoggerFactory.getLogger(Dhis2Producer.class);

    private final Dhis2Endpoint endpoint;
    private final Dhis2Service dhis2Service;

    public Dhis2Producer(Dhis2Endpoint endpoint, Dhis2Service dhis2Service) {
        super(endpoint);
        this.endpoint = endpoint;
        this.dhis2Service = dhis2Service;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String operation = endpoint.getOperation();
        LOG.info("Processing DHIS2 operation: {}", operation);

        // Build request from exchange
        Dhis2Request request = buildRequest(exchange);
        Dhis2Response response;

        // Route to appropriate operation
        switch (operation.toLowerCase()) {
            case "guard":
            case "csvguard":
                response = dhis2Service.validateCsvGuard(request);
                break;

            case "upload":
            case "csvupload":
                response = dhis2Service.uploadCsv(request);
                break;

            case "summarize":
            case "summary":
                response = dhis2Service.summarizeImport(request);
                break;

            case "process":
            case "complete":
            case "map":  // مثل errormapper:map
                response = dhis2Service.processComplete(request);
                break;

            default:
                response = Dhis2Response.error(400, "ERROR", "Unknown operation: " + operation);
        }

        // Set response back to exchange
        setResponse(exchange, response);
    }

    private Dhis2Request buildRequest(Exchange exchange) {
        Dhis2Request request = new Dhis2Request();

        // Get from headers and properties
        request.setTransactionUUID(getStringValue(exchange, "transactionUUID", "X-Transaction-UUID"));
        request.setScheme(getStringValue(exchange, "scheme", endpoint.getIdScheme()));
        request.setStrategy(getStringValue(exchange, "strategy", endpoint.getStrategy()));
        request.setDryRun(getBooleanValue(exchange, "dryRun", "dry-run", endpoint.isDryRun()));
        request.setContentType(exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class));
        request.setOperation(endpoint.getOperation());

        // Get CSV bytes from body or property
        byte[] csvBytes = exchange.getProperty("_csvBytes", byte[].class);
        if (csvBytes == null) {
            csvBytes = exchange.getIn().getBody(byte[].class);
        }
        request.setCsvBytes(csvBytes);

        // For summarize operation, get response body
        if ("summarize".equalsIgnoreCase(endpoint.getOperation())) {
            request.setResponseBody(exchange.getIn().getBody(String.class));
            Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
            request.setHttpStatusCode(statusCode != null ? statusCode : 200);
        }

        // Get query base if available
        request.setQueryBase(exchange.getProperty("_queryBase", String.class));

        return request;
    }

    private void setResponse(Exchange exchange, Dhis2Response response) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, response.getHttpStatusCode());
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");

        // Build JSON response
        String jsonResponse = buildJsonResponse(response);
        exchange.getIn().setBody(jsonResponse);

        // Set properties for further processing
        exchange.setProperty("dhis2.status", response.getStatus());
        exchange.setProperty("dhis2.success", response.isSuccess());

        if (!response.isSuccess()) {
            exchange.setRouteStop(true);
        }
    }

    private String buildJsonResponse(Dhis2Response response) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"status\":\"").append(escape(response.getStatus())).append("\",");
        json.append("\"message\":\"").append(escape(response.getMessage())).append("\",");
        json.append("\"errorCode\":\"").append(escape(response.getErrorCode())).append("\",");
        json.append("\"errorMessage\":\"").append(escape(response.getErrorMessage())).append("\",");
        json.append("\"details\":").append(response.getDetails() != null ? response.getDetails().toString() : "{}");
        json.append("}");
        return json.toString();
    }

    private String getStringValue(Exchange exchange, String... keys) {
        for (String key : keys) {
            String value = exchange.getProperty(key, String.class);
            if (value == null) {
                value = exchange.getIn().getHeader(key, String.class);
            }
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean getBooleanValue(Exchange exchange, String propertyKey, String headerKey, boolean defaultValue) {
        String value = exchange.getProperty(propertyKey, String.class);
        if (value == null) {
            value = exchange.getIn().getHeader(headerKey, String.class);
        }
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()) || "yes".equalsIgnoreCase(value.trim());
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}