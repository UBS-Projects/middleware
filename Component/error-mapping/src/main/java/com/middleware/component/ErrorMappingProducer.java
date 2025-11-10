package com.middleware.component;

import com.middleware.component.model.ErrorMappingDetail;
import com.middleware.component.service.ErrorMappingBridgeService;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer that fetches an error mapping by code and applies it
 * only if the provided raw error message actually matches the defined substring or regex.
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
        // 1️⃣ Extract mapping code
        String code = endpoint.getCode();
        if (code == null || code.isEmpty()) {
            code = exchange.getIn().getHeader("errorMappingCode", String.class);
        }
        if (code == null || code.isEmpty()) {
            LOG.debug("No error mapping code provided — skipping mapping.");
            return;
        }

        // 2️⃣ Extract HTTP status and body
        Integer httpCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (httpCode == null) httpCode = 200;

        if (httpCode < 400) {
            LOG.debug("HTTP {} indicates success — skipping mapping (code={})", httpCode, code);
            return;
        }

        String rawError = extractErrorMessage(exchange);
        if (rawError == null || rawError.isBlank()) {
            LOG.debug("No error message found in exchange body/headers — skipping mapping (code={})", code);
            return;
        }

        // 3️⃣ Fetch mapping definition by code
        ErrorMappingDetail mapping = bridgeService.getErrorMappingByCode(code);
        if (mapping == null) {
            LOG.warn("No error mapping found for code '{}'", code);
            return;
        }

        // 4️⃣ Check if raw error matches the mapping rule
        boolean isMatch = false;
        String substring = mapping.getRawErrorSubstring();
        String matchType = mapping.getMatchType(); // e.g., "CONTAINS", "REGEX", "EQUALS"

        if (matchType == null || substring == null) {
            LOG.debug("Mapping '{}' missing matchType or substring — skipping mapping", code);
            return;
        }

        switch (matchType.toUpperCase()) {
            case "CONTAINS":
                isMatch = rawError.contains(substring);
                break;
            case "EQUALS":
                isMatch = rawError.equals(substring);
                break;
            case "REGEX":
                try {
                    isMatch = rawError.matches(substring);
                } catch (Exception e) {
                    LOG.warn("Invalid regex pattern in mapping '{}': {}", code, e.getMessage());
                }
                break;
        }

        // 5️⃣ Apply or skip
        if (!isMatch) {
            LOG.info("Mapping '{}' found but no match (pattern='{}', rawError='{}') — keeping original error.",
                    code, substring, truncate(rawError, 100));
            return;
        }

        // 6️⃣ Apply mapping
        LOG.info("✅ Applying error mapping '{}' (mappedCode={}, httpStatus={})",
                code, mapping.getMappedErrorCode(), mapping.getHttpStatusCode());

        exchange.getIn().setHeader("MappedErrorCode", mapping.getMappedErrorCode());
        exchange.getIn().setHeader("MappedMessage", mapping.getMappedMessage());
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, mapping.getHttpStatusCode());
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");

        String body = String.format(
                "{\"error\":\"%s\",\"code\":\"%s\"}",
                mapping.getMappedMessage(), mapping.getMappedErrorCode()
        );
        exchange.getIn().setBody(body);
    }

    private String extractErrorMessage(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        if (body != null) return body.toString();

        String headerMsg = exchange.getIn().getHeader("ErrorMessage", String.class);
        return headerMsg != null ? headerMsg : "";
    }

    private String truncate(String text, int limit) {
        if (text == null) return "";
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }
}
