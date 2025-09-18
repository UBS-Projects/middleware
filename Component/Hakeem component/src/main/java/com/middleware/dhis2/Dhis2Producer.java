
package com.middleware.dhis2;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.middleware.dhis2.model.Dhis2Request;
import com.middleware.dhis2.model.Dhis2Response;
import com.middleware.dhis2.service.Dhis2Service;

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

         Dhis2Request request = buildRequestLikeBeans(exchange);
        Dhis2Response response;

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
            case "map":
                response = dhis2Service.processComplete(request);
                break;

            default:
                response = Dhis2Response.error(400, "ERROR", "Unknown operation: " + operation);
        }

         setResponseLikeBeans(exchange, response);
    }

    private Dhis2Request buildRequestLikeBeans(Exchange exchange) {
        Dhis2Request request = new Dhis2Request();

         String transactionUUID = getHeaderValue(exchange, "transactionUUID");
        if (transactionUUID == null) {
            transactionUUID = getHeaderValue(exchange, "X-Transaction-UUID");
        }
        request.setTransactionUUID(transactionUUID);

         String scheme = "CODE"; // default
        String schemeHdr = getHeaderValue(exchange, "scheme");
        if (schemeHdr == null) {
            schemeHdr = getHeaderValue(exchange, "idScheme");
        }
        if (schemeHdr != null && schemeHdr.trim().matches("(?i)^uid$")) scheme = "UID";
        if (schemeHdr != null && schemeHdr.trim().matches("(?i)^code$")) scheme = "CODE";
        request.setScheme(scheme);

         String strategy = "NEW_AND_UPDATES"; // default
        String strategyHdr = getHeaderValue(exchange, "strategy");
        if (strategyHdr != null) {
            String s = strategyHdr.trim();
            if (s.matches("(?i)^append$")) strategy = "NEW";
            else if (s.matches("(?i)^update$")) strategy = "UPDATES";
            else if (s.matches("(?i)^delete$")) strategy = "DELETE";
            else if (s.matches("(?i)^(new_and_updates|new|updates|delete)$")) strategy = s;
        }
        request.setStrategy(strategy);

         String dryRaw = "true"; // default
        String dryHdr1 = getHeaderValue(exchange, "dry-run");
        String dryHdr2 = getHeaderValue(exchange, "dryRun");
        if (!isBlank(dryHdr1)) dryRaw = dryHdr1;
        else if (!isBlank(dryHdr2)) dryRaw = dryHdr2;

        boolean dryRun = toBoolString(dryRaw);
        request.setDryRun(dryRun);

        request.setContentType(exchange.getIn().getHeader(Exchange.CONTENT_TYPE, String.class));
        request.setOperation(endpoint.getOperation());

         byte[] csvBytes = exchange.getIn().getBody(byte[].class);
        request.setCsvBytes(csvBytes);

         if ("summarize".equalsIgnoreCase(endpoint.getOperation())) {
            request.setResponseBody(exchange.getIn().getBody(String.class));
            Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
            request.setHttpStatusCode(statusCode != null ? statusCode : 200);
        }

         String queryBase = buildQueryBaseLikeCsvGuard(scheme, strategy);
        request.setQueryBase(queryBase);

        if (request.getContentType() == null && csvBytes != null) {
            request.setContentType("text/csv");
        }

        LOG.info("Built request - scheme={}, strategy={}, dryRun={}", scheme, strategy, dryRun);

        return request;
    }

    private String buildQueryBaseLikeCsvGuard(String scheme, String strategy) {
        return "async=false" +
                "&preheatCache=false" +
                "&skipAudit=false" +
                "&skipExistingCheck=false" +
                "&firstRowIsHeader=true" +
                "&strategy=" + strategy +
                "&dataElementIdScheme=" + scheme +
                "&orgUnitIdScheme=" + scheme +
                "&categoryOptionComboIdScheme=" + scheme +
                "&attributeOptionComboIdScheme=" + scheme +
                "&idScheme=" + scheme;
    }

    private void setResponseLikeBeans(Exchange exchange, Dhis2Response response) {
         if (!response.isSuccess()) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, response.getHttpStatusCode());
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");

             String jsonResponse;
            if (response.getHttpStatusCode() == 409) {
                 jsonResponse = String.format(
                        "{ \"status\":\"%s\",\"message\":\"%s\",\"errorCode\":\"\",\"errorMessage\":\"\",\"details\":%s }",
                        escape(response.getStatus()),
                        escape(response.getMessage()),
                        response.getDetails() != null ? response.getDetails().toString() : "{}"
                );
            } else {
                 jsonResponse = String.format(
                        "{\"status\":\"%s\",\"message\":\"%s\"}",
                        escape(response.getStatus()),
                        escape(response.getMessage())
                );
            }

            exchange.getIn().setBody(jsonResponse);
            exchange.setRouteStop(true);
            return;
        }

         exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, response.getHttpStatusCode());
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");

        String jsonResponse;
        if (response.getDetails() != null) {
             jsonResponse = String.format(
                    "{ \"status\":\"%s\",\"message\":\"%s\",\"errorCode\":\"\",\"errorMessage\":\"\",\"details\":%s }",
                    escape(response.getStatus()),
                    escape(response.getMessage()),
                    response.getDetails().toString()
            );
        } else {
             jsonResponse = String.format(
                    "{\"status\":\"%s\",\"message\":\"%s\"}",
                    escape(response.getStatus()),
                    escape(response.getMessage())
            );
        }

        exchange.getIn().setBody(jsonResponse);

         exchange.setProperty("dhis2.status", response.getStatus());
        exchange.setProperty("dhis2.success", response.isSuccess());
    }

     private String getHeaderValue(Exchange exchange, String headerName) {
        return exchange.getIn().getHeader(headerName, String.class);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean toBoolString(String raw) {
        if (raw == null) return true;
        String v = raw.trim().toLowerCase();
        return v.matches("^(true|1|yes)$");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}