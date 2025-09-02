package com.middleware.backend.kaotocamel.integrationBeans;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Component("csvGuard")
public class CsvGuard implements Processor {

    private static final Logger log = LoggerFactory.getLogger(CsvGuard.class);
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    );

    @Override
    public void process(Exchange exchange) {
        Message in = exchange.getIn();

        log.info("=== CSV Guard Processing ===");

        // transactionUUID (required - query/header)
        String transactionUUID = str(in.getHeader("transactionUUID", String.class));
        if (transactionUUID == null) {
            transactionUUID = str(in.getHeader("X-Transaction-UUID", String.class));
        }
        exchange.setProperty("transactionUUID", transactionUUID);

        if (isBlank(transactionUUID)) {
            reject(exchange, 400, "ERROR", "Missing required query parameter: transactionUUID");
            return;
        }
        if (!UUID_PATTERN.matcher(transactionUUID.trim()).matches()) {
            reject(exchange, 400, "ERROR", "Invalid transactionUUID format. Expect RFC4122 (e.g., 550e8400-e29b-41d4-a716-446655440000)");
            return;
        }
        in.setHeader("X-Transaction-UUID", transactionUUID.trim());

        String contentTypeHdr = str(in.getHeader(Exchange.CONTENT_TYPE, String.class));
        if (contentTypeHdr != null && contentTypeHdr.toLowerCase().contains("multipart/form-data")) {
            reject(exchange, 415, "ERROR", "Only RAW/Binary CSV is supported (multipart/form-data not allowed)");
            return;
        }

        byte[] csvBytes = in.getBody(byte[].class);
        if (csvBytes == null) {
            String asText = in.getBody(String.class);
            if (isBlank(asText)) {
                reject(exchange, 400, "ERROR", "Empty CSV body");
                return;
            }
            csvBytes = asText.getBytes(StandardCharsets.UTF_8);
        }
        if (csvBytes.length == 0) {
            reject(exchange, 400, "ERROR", "Empty CSV body");
            return;
        }
        in.setBody(csvBytes);
        exchange.setProperty("_csvBytes", csvBytes);

        String scheme = "CODE";
        String schemeHdr = str(in.getHeader("scheme", String.class));
        if (schemeHdr != null && schemeHdr.trim().matches("(?i)^uid$")) scheme = "UID";
        if (schemeHdr != null && schemeHdr.trim().matches("(?i)^code$")) scheme = "CODE";
        exchange.setProperty("scheme", scheme);

        String strategy = "NEW_AND_UPDATES";
        String strategyHdr = str(in.getHeader("strategy", String.class));
        if (strategyHdr != null) {
            String s = strategyHdr.trim();
            if (s.matches("(?i)^append$")) strategy = "NEW";
            else if (s.matches("(?i)^update$")) strategy = "UPDATES";
            else if (s.matches("(?i)^delete$")) strategy = "DELETE";
            else if (s.matches("(?i)^(new_and_updates|new|updates|delete)$")) strategy = s;
        }
        exchange.setProperty("strategy", strategy);

        final String qBase =
                "async=false" +
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
        exchange.setProperty("_queryBase", qBase);

        String dryRaw = "true";
        String dryHdr1 = str(in.getHeader("dry-run", String.class));
        String dryHdr2 = str(in.getHeader("dryRun", String.class));
        if (!isBlank(dryHdr1)) dryRaw = dryHdr1;
        else if (!isBlank(dryHdr2)) dryRaw = dryHdr2;

        String dryRun = toBoolString(dryRaw); // "true"/"false" normalized
        exchange.setProperty("dryRun", dryRun);

        log.info("CSV Guard OK — bytes={}, scheme={}, strategy={}, dryRun={}, qBase={}",
                csvBytes.length, scheme, strategy, dryRun, qBase);
    }

    private static String str(String s) { return s; }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String toBoolString(String raw) {
        if (raw == null) return "true";
        String v = raw.trim().toLowerCase();
        return (v.matches("^(true|1|yes)$")) ? "true" : "false";
    }

    private static void reject(Exchange ex, int code, String status, String message) {
        Message in = ex.getIn();
        in.setHeader(Exchange.HTTP_RESPONSE_CODE, code);
        in.setHeader(Exchange.CONTENT_TYPE, "application/json");
        in.setBody("{\"status\":\"" + status + "\",\"message\":\"" + escape(message) +
                "\",\"errorCode\":\"\",\"errorMessage\":\"\",\"details\":{}}");
        ex.setRouteStop(true);
    }

    private static String escape(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
