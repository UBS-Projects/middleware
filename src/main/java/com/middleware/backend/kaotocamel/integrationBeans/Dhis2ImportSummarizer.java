package com.middleware.backend.kaotocamel.integrationBeans;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("dhis2ImportSummarizer")
public class Dhis2ImportSummarizer implements Processor {

    private static final Logger log = LoggerFactory.getLogger(Dhis2ImportSummarizer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getMessage();
        String body = in.getBody(String.class);

        if (body == null || body.isBlank()) {
            fail(exchange, 502, "Empty response from DHIS2");
            return;
        }

        if (body.trim().startsWith("{") && body.contains("\"status\"")) {
            in.setHeader(Exchange.CONTENT_TYPE, "application/json");
            return;
        }

        try {
            JsonNode root = MAPPER.readTree(body);
            Map<String, Object> wrapper = new LinkedHashMap<>();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("status", root.path("status").asText("UNKNOWN"));
            summary.put("hasConflicts", root.path("response").path("conflicts").isArray()
                    && root.path("response").path("conflicts").size() > 0);
            summary.put("errorCode", "");
            summary.put("errorMessage", "");
            wrapper.put("summary", summary);
            wrapper.put("details", root);
            in.setBody(MAPPER.writeValueAsString(wrapper));
            in.setHeader(Exchange.CONTENT_TYPE, "application/json");
        } catch (Exception e) {
            fail(exchange, 502, "Invalid JSON from DHIS2: " + e.getMessage());
        }
    }

    private static void fail(Exchange ex, int code, String msg) {
        Message in = ex.getMessage();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", "ERROR");
        summary.put("hasConflicts", false);
        summary.put("errorCode", "");
        summary.put("errorMessage", msg);
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("summary", summary);
        wrapper.put("details", Map.of("message", msg));
        try {
            in.setBody(MAPPER.writeValueAsString(wrapper));
        } catch (Exception ignored) {
            in.setBody("{\"summary\":{\"status\":\"ERROR\",\"errorMessage\":\"" + msg.replace("\"", "\\\"") + "\"}}");
        }
        in.setHeader(Exchange.HTTP_RESPONSE_CODE, code);
        in.setHeader(Exchange.CONTENT_TYPE, "application/json");
    }
}
