//package com.middleware.backend.config;
//
//import org.apache.camel.Exchange;
//import org.apache.camel.impl.DefaultCamelContext;
//import org.apache.camel.support.DefaultExchange;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import com.middleware.backend.kaotocamel.integrationBeans.CsvGuard;
//import com.middleware.backend.kaotocamel.integrationBeans.Dhis2CsvDryRunThenCommit;
//import com.middleware.backend.kaotocamel.integrationBeans.Dhis2ImportSummarizer;
//import com.middleware.dhis2.model.Dhis2Request;
//import com.middleware.dhis2.model.Dhis2Response;
//import com.middleware.dhis2.service.Dhis2Service;
//
///**
// * Configuration class for setting up DHIS2 related components and services.
// * This class defines beans that encapsulate the logic for interacting with a DHIS2 instance,
// * including data validation, uploading, and processing import summaries. It leverages
// * custom Camel integration beans to perform these operations within a Camel route context.
// */
//@Configuration
//public class Dhis2ComponentConfiguration {
//
//    private static final Logger LOG = LoggerFactory.getLogger(Dhis2ComponentConfiguration.class);
//
//    @Autowired
//    private CsvGuard csvGuard;
//
//    @Autowired
//    private Dhis2CsvDryRunThenCommit dhis2Upload;
//
//    @Autowired
//    private Dhis2ImportSummarizer importSummarizer;
//
//    /**
//     * Creates and configures the {@link Dhis2Service} bean.
//     * This service provides a high-level interface for performing DHIS2 operations
//     * such as CSV validation, data upload, and import summarization.
//     *
//     * @return An implementation of the {@link Dhis2Service}.
//     */
//    @Bean(name = "dhis2ComponentService")
//    public Dhis2Service dhis2ComponentServiceImplementation() {
//        return new Dhis2Service() {
//
//            /**
//             * Validates a CSV file using the CsvGuard component.
//             *
//             * @param request The DHIS2 request containing the CSV data.
//             * @return A {@link Dhis2Response} indicating the result of the validation.
//             */
//            @Override
//            public Dhis2Response validateCsvGuard(Dhis2Request request) {
//                try {
//                    LOG.info("Executing CSV Guard validation with actual logic");
//
//                    if (request.getCsvBytes() == null || request.getCsvBytes().length == 0) {
//                        LOG.error("CSV Guard validation failed: Empty CSV body");
//                        return Dhis2Response.error(400, "ERROR", "Empty CSV body");
//                    }
//
//                    Exchange exchange = createMockExchangeFromRequest(request);
//                    csvGuard.process(exchange);
//
//                    Boolean routeStop = exchange.getProperty(Exchange.ROUTE_STOP, Boolean.class);
//                    if (routeStop != null && routeStop) {
//                        Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
//
//                        return createResponseFromExchange(exchange, statusCode != null ? statusCode : 400);
//                    }
//
//                    return Dhis2Response.success("CSV validation completed successfully", null);
//
//                } catch (Exception e) {
//                    LOG.error("CSV Guard validation failed: {}", e.getMessage(), e);
//                    return Dhis2Response.error(400, "ERROR", "Validation failed: " + e.getMessage());
//                }
//            }
//
//            /**
//             * Uploads a CSV file to DHIS2.
//             *
//             * @param request The DHIS2 request containing the CSV data and other parameters.
//             * @return A {@link Dhis2Response} with the result of the upload operation.
//             */
//            @Override
//            public Dhis2Response uploadCsv(Dhis2Request request) {
//                try {
//                    LOG.info("Executing CSV upload with actual DHIS2 logic");
//
//                    Exchange exchange = createMockExchangeFromRequest(request);
//
//                    if (request.getCsvBytes() != null) {
//                        exchange.setProperty("_csvBytes", request.getCsvBytes());
//                    } else {
//                        LOG.error("CSV bytes missing for upload");
//                        return Dhis2Response.error(500, "ERROR", "Internal processing state is missing (_csvBytes)");
//                    }
//
//                    if (request.getQueryBase() != null) {
//                        exchange.setProperty("_queryBase", request.getQueryBase());
//                    } else {
//                        LOG.error("Query base missing for upload");
//                        return Dhis2Response.error(500, "ERROR", "Internal processing state is missing (_queryBase)");
//                    }
//
//                    exchange.setProperty("dryRun", String.valueOf(request.isDryRun()));
//
//                    dhis2Upload.process(exchange);
//
//                    Boolean routeStop = exchange.getProperty(Exchange.ROUTE_STOP, Boolean.class);
//                    if (routeStop != null && routeStop) {
//                        Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
//                        return createResponseFromExchange(exchange, statusCode != null ? statusCode : 500);
//                    }
//
//                    String responseBody = exchange.getIn().getBody(String.class);
//                    Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
//
//                    if (statusCode != null && statusCode >= 400) {
//                        return createResponseFromExchange(exchange, statusCode);
//                    }
//
//                    Object details = parseJsonSafely(responseBody);
//                    return Dhis2Response.success("CSV upload completed successfully", details);
//
//                } catch (Exception e) {
//                    LOG.error("CSV upload failed: {}", e.getMessage(), e);
//                    return Dhis2Response.error(500, "ERROR", "Upload failed: " + e.getMessage());
//                }
//            }
//
//            /**
//             * Summarizes the import results from a DHIS2 response.
//             *
//             * @param request The DHIS2 request containing the response body from a previous import.
//             * @return A {@link Dhis2Response} with the summarized import status.
//             */
//            @Override
//            public Dhis2Response summarizeImport(Dhis2Request request) {
//                try {
//                    LOG.info("Executing import summary with actual logic");
//
//                    Exchange exchange = createMockExchangeFromRequest(request);
//
//                    if (request.getResponseBody() != null) {
//                        exchange.getIn().setBody(request.getResponseBody());
//                    }
//
//                    importSummarizer.process(exchange);
//
//                    String responseBody = exchange.getIn().getBody(String.class);
//                    Integer statusCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
//
//                    if (statusCode != null && statusCode >= 400) {
//                        return createResponseFromExchange(exchange, statusCode);
//                    }
//
//                    Object details = parseJsonSafely(responseBody);
//                    return Dhis2Response.success("Import summarized successfully", details);
//
//                } catch (Exception e) {
//                    LOG.error("Import summarization failed: {}", e.getMessage(), e);
//                    return Dhis2Response.error(500, "ERROR", "Summarization failed: " + e.getMessage());
//                }
//            }
//
//            /**
//             * Executes the complete DHIS2 processing pipeline: validate, upload, and summarize.
//             *
//             * @param request The DHIS2 request to process.
//             * @return A {@link Dhis2Response} with the final result of the pipeline.
//             */
//            @Override
//            public Dhis2Response processComplete(Dhis2Request request) {
//                try {
//                    LOG.info("Starting complete DHIS2 processing pipeline");
//
//                    Dhis2Response guardResponse = validateCsvGuard(request);
//                    if (!guardResponse.isSuccess()) {
//                        return guardResponse;
//                    }
//
//                    Exchange guardExchange = createMockExchangeFromRequest(request);
//                    try {
//                        csvGuard.process(guardExchange);
//                        byte[] csvBytes = guardExchange.getProperty("_csvBytes", byte[].class);
//                        String queryBase = guardExchange.getProperty("_queryBase", String.class);
//                        String dryRun = guardExchange.getProperty("dryRun", String.class);
//
//                        if (csvBytes != null) request.setCsvBytes(csvBytes);
//                        if (queryBase != null) request.setQueryBase(queryBase);
//                        if (dryRun != null) request.setDryRun("true".equals(dryRun));
//
//                    } catch (Exception e) {
//                        LOG.warn("Failed to re-run CSV Guard for property extraction: {}", e.getMessage());
//                    }
//
//                    // Step 2: CSV upload
//                    Dhis2Response uploadResponse = uploadCsv(request);
//                    if (!uploadResponse.isSuccess()) {
//                        return uploadResponse;
//                    }
//
//                    if (uploadResponse.getDetails() != null) {
//                        request.setResponseBody(uploadResponse.getDetails().toString());
//                    }
//                    Dhis2Response summaryResponse = summarizeImport(request);
//
//                    return summaryResponse;
//
//                } catch (Exception e) {
//                    LOG.error("Complete DHIS2 processing failed: {}", e.getMessage(), e);
//                    return Dhis2Response.error(500, "ERROR", "Complete processing failed: " + e.getMessage());
//                }
//            }
//
//            /**
//             * Creates a mock Camel {@link Exchange} from a {@link Dhis2Request}.
//             *
//             * @param request The source request.
//             * @return A new {@link Exchange} instance populated with data from the request.
//             */
//            private Exchange createMockExchangeFromRequest(Dhis2Request request) {
//                Exchange exchange = new DefaultExchange(new DefaultCamelContext());
//
//                if (request.getTransactionUUID() != null) {
//                    exchange.getIn().setHeader("transactionUUID", request.getTransactionUUID());
//                    exchange.getIn().setHeader("X-Transaction-UUID", request.getTransactionUUID());
//                }
//
//                if (request.getScheme() != null) {
//                    exchange.getIn().setHeader("scheme", request.getScheme());
//                }
//
//                if (request.getStrategy() != null) {
//                    exchange.getIn().setHeader("strategy", request.getStrategy());
//                }
//
//                exchange.getIn().setHeader("dryRun", String.valueOf(request.isDryRun()));
//
//                if (request.getContentType() != null) {
//                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, request.getContentType());
//                }
//
//                if (request.getCsvBytes() != null) {
//                    exchange.getIn().setBody(request.getCsvBytes());
//                } else {
//                    exchange.getIn().setBody((String) null);
//                }
//
//                return exchange;
//            }
//
//            /**
//             * Creates a {@link Dhis2Response} from a Camel {@link Exchange}.
//             *
//             * @param exchange   The exchange containing the response data.
//             * @param statusCode The HTTP status code to set in the response.
//             * @return A new {@link Dhis2Response} instance.
//             */
//            private Dhis2Response createResponseFromExchange(Exchange exchange, int statusCode) {
//                String body = exchange.getIn().getBody(String.class);
//                Object details = parseJsonSafely(body);
//
//                String status = (statusCode == 409) ? "CONFLICT" :
//                        (statusCode >= 400) ? "ERROR" : "SUCCESS";
//
//                return new Dhis2Response() {{
//                    setStatus(status);
//                    setMessage(extractMessageFromJson(body));
//                    setDetails(details);
//                    setHttpStatusCode(statusCode);
//                    setSuccess(statusCode < 400);
//                }};
//            }
//
//            /**
//             * Safely parses a string into a JSON object.
//             *
//             * @param jsonString The string to parse.
//             * @return The parsed JSON object or an escaped string if parsing fails.
//             */
//            private Object parseJsonSafely(String jsonString) {
//                if (jsonString == null || jsonString.trim().isEmpty()) {
//                    return "{}";
//                }
//
//                String trimmed = jsonString.trim();
//                if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
//                        (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
//                    try {
//                        // Try to parse as JSON to validate
//                        return jsonString;
//                    } catch (Exception e) {
//                        return "\"" + escape(jsonString) + "\"";
//                    }
//                }
//
//                return "\"" + escape(jsonString) + "\"";
//            }
//
//            /**
//             * Extracts the 'message' field from a JSON string.
//             *
//             * @param jsonString The JSON string to search.
//             * @return The extracted message or a default string.
//             */
//            private String extractMessageFromJson(String jsonString) {
//                if (jsonString == null) return "";
//
//                // Simple regex to extract message field
//                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
//                java.util.regex.Matcher matcher = pattern.matcher(jsonString);
//
//                if (matcher.find()) {
//                    return matcher.group(1);
//                }
//
//                return "Operation completed";
//            }
//
//            /**
//             * Escapes special characters in a string.
//             *
//             * @param s The string to escape.
//             * @return The escaped string.
//             */
//            private String escape(String s) {
//                if (s == null) return "";
//                return s.replace("\\", "\\\\").replace("\"", "\\\"");
//            }
//        };
//    }
//}