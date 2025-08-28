 package com.middleware.backend.kaotocamel.integrationBeans;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component("csvGuard")
public class CsvGuard implements Processor {

    @Override
    public void process(Exchange exchange) {
        Message in = exchange.getIn();

        // 0) ارفض multipart
        String contentTypeHdr = headerString(in, Exchange.CONTENT_TYPE);
        if (contentTypeHdr != null && contentTypeHdr.toLowerCase().contains("multipart/form-data")) {
            reject(exchange, 415, "Only RAW/Binary CSV is supported (multipart/form-data not allowed)");
            return;
        }

        // 1) اقرأ الجسم كـ byte[] أو String
        byte[] bodyBytes = in.getBody(byte[].class);
        String bodyStr = null;

        if (bodyBytes != null) {
            bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);
        } else {
            bodyStr = in.getBody(String.class);
            if (bodyStr != null) bodyBytes = bodyStr.getBytes(StandardCharsets.UTF_8);
        }

        // 2) فحص الفراغ
        boolean contentLengthZero = false;
        Object cl = in.getHeader("Content-Length");
        if (cl != null) {
            try { contentLengthZero = Long.parseLong(String.valueOf(cl)) == 0L; } catch (NumberFormatException ignore) {}
        }
        boolean isEmpty =
                (bodyBytes == null || bodyBytes.length == 0) ||
                        (bodyStr == null || bodyStr.trim().isEmpty()) ||
                        contentLengthZero;

        if (isEmpty) {
            reject(exchange, 400, "Empty CSV body");
            return;
        }

        // 3) ثبّت البودي كـ byte[] وخزّنه للـ Commit
        in.setBody(bodyBytes);
        exchange.setProperty("_csvBytes", bodyBytes);

        // 4) CamelFileName افتراضي
        String fileName = headerString(in, "CamelFileName");
        if (fileName == null || fileName.isBlank()) {
            in.setHeader("CamelFileName", "uploaded.csv");
        }

        // 5) scheme/strategy
        String scheme   = normalizeScheme(headerString(in, "scheme"));   // UID|CODE|NAME
        String strategy = normalizeStrategy(headerString(in, "strategy"));// MERGE|APPEND|UPDATE|DELETE|...

        // 6) Query base — بدون dryRun، ومع format=csv
        String queryBase =
                "async=false" +
                        "&format=csv" +
                        "&strategy=" + url(strategy) +
                        "&preheatCache=false" +
                        "&skipAudit=false" +
                        "&skipExistingCheck=false" +
                        "&firstRowIsHeader=true" +
                        "&dataElementIdScheme=" + url(scheme) +
                        "&orgUnitIdScheme=" + url(scheme) +
                        "&categoryOptionComboIdScheme=" + url(scheme) +
                        "&attributeOptionComboIdScheme=" + url(scheme) +
                        "&idScheme=" + url(scheme);

        exchange.setProperty("_queryBase", queryBase);

        // 7) هيدرز افتراضية (سيعاد تثبيتها في YAML قبل الاستدعاء)
        in.setHeader(Exchange.HTTP_METHOD, "POST");
        in.setHeader(Exchange.CONTENT_TYPE, "application/csv"); // ✅ مهم
        in.setHeader("Accept", "application/json");
    }

    private static String headerString(Message in, String name) {
        Object v = in.getHeader(name);
        return v == null ? null : String.valueOf(v);
    }

    private static void reject(Exchange exchange, int httpCode, String msg) {
        Message in = exchange.getIn();
        in.setHeader(Exchange.HTTP_RESPONSE_CODE, httpCode);
        in.setHeader(Exchange.CONTENT_TYPE, "application/json");
        in.setBody("{\"success\":false,\"error\":\"" + escapeJson(msg) + "\"}");
        exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
        in.setHeader("CamelRouteStop", "true");
        exchange.setRouteStop(true);
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }

    private static String normalizeScheme(String schemeHeader) {
        String s = (schemeHeader == null || schemeHeader.isBlank()) ? "CODE" : schemeHeader.trim().toUpperCase();
        return s.matches("UID|CODE|NAME") ? s : "CODE";
    }

    private static String normalizeStrategy(String strategyHeader) {
        String s = (strategyHeader == null || strategyHeader.isBlank()) ? "NEW_AND_UPDATES" : strategyHeader.trim().toUpperCase();
        switch (s) {
            case "MERGE":  return "NEW_AND_UPDATES";
            case "APPEND": return "NEW";
            case "UPDATE": return "UPDATES";
            case "DELETE": return "DELETE";
            default: return s.matches("NEW_AND_UPDATES|NEW|UPDATES|DELETE") ? s : "NEW_AND_UPDATES";
        }
    }

    private static String url(String v) { return v == null ? "" : v; }
}
