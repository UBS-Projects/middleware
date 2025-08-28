package com.middleware.backend.kaotocamel.integrationBeans;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("dhis2ImportSummarizer")
public class Dhis2ImportSummarizer implements Processor {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getMessage();

         String body = safeString(in.getBody(String.class));
        if (body == null || body.isBlank()) {
            fail(exchange, 502, "Empty response from DHIS2");
            return;
        }

        JsonNode root;
        try {
            root = om.readTree(body);
        } catch (Exception parseEx) {
            fail(exchange, 502, "Invalid JSON from DHIS2");
            return;
        }

         String topStatus  = text(root, "status");                 // OK / WARNING / ERROR
        int topCode       = intval(root, "httpStatusCode", 200);

         JsonNode resp     = root.path("response");
        String respStatus = text(resp, "status");                 // SUCCESS / WARNING / ERROR

         boolean dryRunInPayload = resp.path("importOptions").path("dryRun").asBoolean(false);
        boolean dryRunHeader    = boolHeader(in, "X-Dry-Run"); // optional helper header
        boolean isDryRun        = dryRunInPayload || dryRunHeader;

         JsonNode conflicts = resp.path("conflicts");
        int conflictsCount = (conflicts.isArray()) ? conflicts.size() : 0;
        boolean hasConflicts = conflictsCount > 0;

         int clientCode = decideStatusCode(topStatus, topCode, respStatus, hasConflicts);

         if (isDryRun && hasConflicts) {
            Map<String, Object> wrapper = new LinkedHashMap<>();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("status", chooseUserStatus(respStatus, topStatus));
            summary.put("hasConflicts", true);
            summary.put("conflictsCount", conflictsCount);
             summary.put("description", "Conflicts detected. Fix them and retry.");
            wrapper.put("summary", summary);
            wrapper.put("details", root);

            String out = om.writeValueAsString(wrapper);
            in.setBody(out);
            in.setHeader(Exchange.HTTP_RESPONSE_CODE, 409);
            in.setHeader("Content-Type", "application/json");
             in.setHeader(Exchange.ROUTE_STOP, true);
            return;
        }

         Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", chooseUserStatus(respStatus, topStatus));
        summary.put("hasConflicts", hasConflicts);
        summary.put("conflictsCount", conflictsCount);
         summary.put("description", descriptionOrFallback(resp.path("description").asText(null), hasConflicts));

        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("summary", summary);
        wrapper.put("details", root);

        in.setBody(om.writeValueAsString(wrapper));
        in.setHeader(Exchange.HTTP_RESPONSE_CODE, clientCode);
        in.setHeader("Content-Type", "application/json");
    }


    private static String descriptionOrFallback(String description, boolean hasConflicts) {
        if (hasConflicts) {
             return "Conflicts detected. Fix them and retry.";
        }
        return (description != null && !description.isBlank())
                ? description
                : "Import process completed.";
    }

    private static String chooseUserStatus(String respStatus, String topStatus) {
        if (notBlank(respStatus)) return respStatus;
        if (notBlank(topStatus)) return topStatus;
        return "UNKNOWN";
    }

    private static int decideStatusCode(String topStatus, int topCode, String respStatus, boolean hasConflicts) {
         if (hasConflicts) return 409;

        String rs = (respStatus == null) ? "" : respStatus.toUpperCase();
        String ts = (topStatus == null) ? "" : topStatus.toUpperCase();

        if ("SUCCESS".equals(rs) || "OK".equals(ts)) {
            return 200;
        }
        if ("WARNING".equals(rs) || "WARNING".equals(ts)) {
             return 200;
        }
        if ("ERROR".equals(rs) || "ERROR".equals(ts)) {
            return 500;
        }
         return topCode > 0 ? topCode : 200;
    }

    private static boolean boolHeader(Message in, String name) {
        String v = in.getHeader(name, String.class);
        if (v == null) return false;
        v = v.trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v);
    }

    private static String text(JsonNode n, String f) {
        JsonNode v = (n == null) ? null : n.path(f);
        return (v == null || v.isMissingNode() || v.isNull()) ? null : v.asText();
    }

    private static int intval(JsonNode n, String f, int d) {
        JsonNode v = (n == null) ? null : n.path(f);
        return (v == null || v.isMissingNode() || v.isNull()) ? d : v.asInt(d);
    }

    private static String safeString(String s) {
        return s == null ? null : s;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static void fail(Exchange ex, int code, String msg) {
        Message in = ex.getMessage();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", "ERROR");
        summary.put("hasConflicts", false);
        summary.put("conflictsCount", 0);
        summary.put("description", msg);

        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("summary", summary);
        wrapper.put("details", Map.of("message", msg));

        try {
            in.setBody(new ObjectMapper().writeValueAsString(wrapper));
        } catch (Exception ignored) {
            in.setBody("{\"summary\":{\"status\":\"ERROR\",\"description\":\"" + msg + "\"}}");
        }
        in.setHeader(Exchange.HTTP_RESPONSE_CODE, code);
        in.setHeader("Content-Type", "application/json");
    }
}
