package com.middleware.backend.kaotocamel.integrationBeans;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("dhis2ImportSummarizer")
public class Dhis2ImportSummarizer implements Processor {

    private static final Logger log = LoggerFactory.getLogger(Dhis2ImportSummarizer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void process(Exchange exchange) {
        Message in = exchange.getMessage();
        String body = in.getBody(String.class);

        if (body == null || body.isBlank()) {
            writeShort(exchange, 502, "GLOBAL_ERROR", "Empty response from DHIS2");
            return;
        }

        String t = body.trim();

        if (t.startsWith("{") && t.contains("\"status\"") && t.contains("\"message\"")) {
            in.setHeader(Exchange.CONTENT_TYPE, "application/json");
            return;
        }

        try {
            JsonNode root = MAPPER.readTree(t);

            String status = inferStatus(root); // SUCCESS / CONFLICT / ERROR / GLOBAL_ERROR
            String message = inferMessage(root, status);

            String unified = "{"
                    + "\"status\":\"" + esc(status) + "\","
                    + "\"message\":\"" + esc(message) + "\","
                    + "\"errorCode\":\"\","
                    + "\"errorMessage\":\"\","
                    + "\"details\":" + t
                    + "}";

            in.setHeader(Exchange.CONTENT_TYPE, "application/json");
            in.setHeader(Exchange.HTTP_RESPONSE_CODE, pickHttpCode(status));
            in.setBody(unified);
        } catch (Exception e) {
            log.warn("Invalid DHIS2 JSON at summarizer: {}", e.getMessage());
            writeShort(exchange, 502, "GLOBAL_ERROR", "Invalid JSON from DHIS2");
        }
    }

    // === Helpers ===

    private static void writeShort(Exchange ex, int http, String status, String message) {
        Message in = ex.getMessage();
        in.setHeader(Exchange.HTTP_RESPONSE_CODE, http);
        in.setHeader(Exchange.CONTENT_TYPE, "application/json");
        in.setBody("{\"status\":\"" + esc(status) + "\",\"message\":\"" + esc(message) + "\"}");
    }

    private static int pickHttpCode(String status) {
        return switch (status) {
            case "SUCCESS" -> 200;
            case "CONFLICT" -> 409;
            case "ERROR" -> 400;
            default -> 500; // GLOBAL_ERROR
        };
    }

    private static String inferStatus(JsonNode root) {
        String s = root.path("status").asText("").toUpperCase();
        if ("OK".equals(s) || "SUCCESS".equals(s)) return "SUCCESS";
        if ("ERROR".equals(s) || "FAILED".equals(s) || "FAIL".equals(s)) return "ERROR";
        int http = root.path("httpStatusCode").asInt(0);
        if (http == 409) return "CONFLICT";
        if (http >= 200 && http <= 202) return "SUCCESS";
        if (http >= 400 && http < 500) return "ERROR";
        if (http >= 500) return "GLOBAL_ERROR";
        if ("Conflict".equalsIgnoreCase(root.path("httpStatus").asText())) return "CONFLICT";
        if (root.path("response").path("conflicts").isArray() && root.path("response").path("conflicts").size() > 0)
            return "CONFLICT";
        return "GLOBAL_ERROR";
    }

    private static String inferMessage(JsonNode root, String status) {
        if ("CONFLICT".equals(status)) return "Resolve the conflicts then re-upload";
        if ("SUCCESS".equals(status)) return "completed successfully";
        String msg = firstNonBlank(
                root.path("message").asText(""),
                root.path("error").asText(""),
                root.path("response").path("message").asText(""),
                root.path("importStatus").asText(""),
                root.path("description").asText("")
        );
        if (msg.isBlank()) {
            if ("ERROR".equals(status)) return "Operation failed";
            return "Unknown DHIS2 response";
        }
        return msg;
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
