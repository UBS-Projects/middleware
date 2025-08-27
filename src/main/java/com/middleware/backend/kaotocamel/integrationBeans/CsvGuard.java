package com.middleware.backend.kaotocamel.integrationBeans;


import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
@Component("csvGuard")

public class CsvGuard implements Processor {
    private static final Pattern MULTIPART = Pattern.compile("(?i).*multipart/form-data.*");

    @Override public void process(Exchange ex) {
        Message in = ex.getMessage();

         String ct = null;
        Object req = in.getHeader("CamelHttpServletRequest");
        if (req != null) {
            try {
                ct = (String) req.getClass().getMethod("getContentType").invoke(req);
            } catch (Exception ignored) {}
        }
        if (ct == null) ct = in.getHeader("Content-Type", String.class);
        if (ct != null && MULTIPART.matcher(ct).matches()) {
            in.setHeader(Exchange.HTTP_RESPONSE_CODE, 415);
            in.setHeader("Content-Type", "application/json");
            in.setBody("{\"success\":false,\"error\":\"Only RAW/Binary CSV is supported (multipart/form-data not allowed)\"}");
            in.setHeader(Exchange.ROUTE_STOP, true);
            return;
        }

         String fileName = in.getHeader(Exchange.FILE_NAME, String.class);
        if (fileName == null || fileName.isEmpty()) {
            in.setHeader(Exchange.FILE_NAME, "uploaded.csv");
            fileName = "uploaded.csv";
        }

         String csv = in.getBody(String.class);
        if (csv == null || csv.trim().isEmpty()) {
            in.setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            in.setHeader("Content-Type", "application/json");
            in.setBody("{\"success\":false,\"error\":\"Empty CSV body\",\"fileName\":\"" + fileName + "\"}");
            in.setHeader(Exchange.ROUTE_STOP, true);
            return;
        }

         String scheme = "CODE";
        String schemeHdr = in.getHeader("scheme", String.class);
        if (schemeHdr != null && !schemeHdr.isEmpty()) {
            String up = schemeHdr.toUpperCase(Locale.ROOT);
            if (up.equals("UID") || up.equals("CODE") || up.equals("NAME")) scheme = up;
        }

         String strategy = "NEW_AND_UPDATES";
        String strategyHdr = in.getHeader("strategy", String.class);
        if (strategyHdr != null && !strategyHdr.isEmpty()) {
            String up = strategyHdr.toUpperCase(Locale.ROOT);
            switch (up) {
                case "MERGE":  strategy = "NEW_AND_UPDATES"; break;
                case "APPEND": strategy = "NEW"; break;
                case "UPDATE": strategy = "UPDATES"; break;
                default:
                    if (up.matches("^(NEW_AND_UPDATES|NEW|UPDATES|DELETE)$")) strategy = up;
            }
        }

         String qs = "async=false&dryRun=false"
                + "&strategy=" + strategy
                + "&preheatCache=false&skipAudit=false&skipExistingCheck=false&firstRowIsHeader=true"
                + "&dataElementIdScheme=" + scheme
                + "&orgUnitIdScheme=" + scheme
                + "&categoryOptionComboIdScheme=" + scheme
                + "&attributeOptionComboIdScheme=" + scheme
                + "&idScheme=" + scheme;
        in.setHeader(Exchange.HTTP_QUERY, qs);

         in.setBody(csv.getBytes(StandardCharsets.UTF_8));
        in.setHeader(Exchange.HTTP_METHOD, "POST");
        in.setHeader("Content-Type", "application/csv");
        in.setHeader("Accept", "*/*");
    }
}