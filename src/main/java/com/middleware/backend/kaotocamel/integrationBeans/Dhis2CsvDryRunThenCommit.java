package com.middleware.backend.kaotocamel.integrationBeans;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Component("dhis2CsvDryRunThenCommit")
public class Dhis2CsvDryRunThenCommit implements Processor {

    private static final Logger log = LoggerFactory.getLogger(Dhis2CsvDryRunThenCommit.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${dhis2.base-url}")
    private String baseUrl;

    @Value("${dhis2.username}")
    private String username;

    @Value("${dhis2.password}")
    private String password;

    private volatile int lastResponseCode = 0;

    @Override
    public void process(Exchange exchange) {
        Message in = exchange.getIn();

        log.info("=== DHIS2 CSV Upload Starting ===");

        byte[] csv = exchange.getProperty("_csvBytes", byte[].class);
        String qBase = exchange.getProperty("_queryBase", String.class);
        String dryRun = exchange.getProperty("dryRun", String.class); // "true" / "false"

        if (csv == null || qBase == null) {
            writeError(exchange, 500, "Internal processing state is missing (_csvBytes/_queryBase)");
            return;
        }

        boolean isDryRunMode = !"false".equalsIgnoreCase(dryRun); // default true
        log.info("CSV size={} bytes, dryRunMode={}", csv.length, isDryRunMode);

        try {
            if (isDryRunMode) {
                String dryResp = callDhis2(csv, qBase, true);
                int dryCode = getLastResponseCode();
                log.info("Dry-run httpCode={}, bodyPreview={}", dryCode, preview(dryResp));

                if (dryResp == null || dryCode == 0) {
                    setJson(exchange, 500,
                            "{\"summary\":{\"status\":\"ERROR\",\"hasConflicts\":false,\"conflictsCount\":0," +
                                    "\"description\":\"Import process completed.\"}," +
                                    "\"details\":{\"status\":\"ERROR\",\"message\":\"DHIS2 unreachable (no HTTP status in dry-run)\"," +
                                    "\"errorCode\":\"\",\"errorMessage\":\"\",\"details\":{\"raw\":" + rawAsJson(exchange) + "}}}");
                    exchange.setRouteStop(true);
                    return;
                }

                boolean hasConflict = checkConflictsYamlLogic(dryResp, dryCode);
                if (hasConflict) {
                    log.warn("Dry-run conflicts detected — stopping before commit");
                    setJson(exchange, 409,
                            "{ \"status\":\"CONFLICT\",\"message\":\"Resolve the conflicts then re-upload\"," +
                                    "\"errorCode\":\"\",\"errorMessage\":\"\",\"details\":" + safeJson(dryResp) + " }");
                    exchange.setRouteStop(true);
                    return;
                }

                String commitResp = callDhis2(csv, qBase, false);
                int commitCode = getLastResponseCode();
                log.info("Commit httpCode={}, bodyPreview={}", commitCode, preview(commitResp));

                if (commitResp == null || commitCode == 0) {
                    writeError(exchange, 500, "Failed to get commit response from DHIS2");
                    return;
                }

                String status = (commitCode >= 200 && commitCode <= 202) ? "SUCCESS" : "ERROR";
                setJson(exchange, 200,
                        "{ \"status\":\"" + status + "\",\"message\":\"completed successfully\"," +
                                "\"errorCode\":\"\",\"errorMessage\":\"\",\"details\":" + safeJson(commitResp) + " }");
                exchange.setRouteStop(true);
                return;

            } else {
                String resp = callDhis2(csv, qBase, false);
                int code = getLastResponseCode();
                log.info("Direct import httpCode={}, bodyPreview={}", code, preview(resp));

                if (resp == null || code == 0) {
                    writeError(exchange, 500, "Failed to get response from DHIS2");
                    return;
                }

                String status = (code == 409) ? "CONFLICT"
                        : (code >= 200 && code <= 202) ? "SUCCESS"
                        : "ERROR";

                setJson(exchange, code,
                        "{ \"status\":\"" + status + "\",\"message\":\"completed (no dry-run)\"," +
                                "\"errorCode\":\"\",\"errorMessage\":\"\",\"details\":" + safeJson(resp) + " }");
                exchange.setRouteStop(true);
                return;
            }
        } catch (Exception e) {
            log.error("Error during DHIS2 upload process: {}", e.getMessage(), e);
            writeError(exchange, 500, "Failed to call DHIS2: " + e.getMessage());
        }
    }

    private String callDhis2(byte[] csv, String qBase, boolean dryRun) {
        HttpURLConnection conn = null;
        try {
            String fullUrl = "https://" + baseUrl + "/api/dataValueSets?dryRun=" + (dryRun ? "true" : "false") + "&" + qBase;
            log.info("Calling DHIS2 {}", fullUrl);

            disableSSLVerification();

            URL url = new URL(fullUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String auth = "Basic " + Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8)
            );
            conn.setRequestProperty("Authorization", auth);
            conn.setRequestProperty("Content-Type", "application/csv");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Middleware-DHIS2/1.0");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(csv);
                os.flush();
            }

            lastResponseCode = safeCode(conn);
            InputStream is = (lastResponseCode >= 200 && lastResponseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
            String body = readAll(is);

            return body;
        } catch (Exception e) {
            log.error("Error calling DHIS2: {}", e.getMessage(), e);
            lastResponseCode = 0;
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private int getLastResponseCode() { return lastResponseCode; }

    private static int safeCode(HttpURLConnection c) {
        try { return c.getResponseCode(); }
        catch (Exception e) { return 0; }
    }

    private static boolean checkConflictsYamlLogic(String response, int responseCode) {
        if (responseCode == 409) return true;
        if (response == null) return false;

        if (response.matches("(?i).*\"httpStatus\"\\s*:\\s*\"Conflict\".*")) return true;
        if (response.matches("(?is).*\"conflicts\"\\s*:\\s*\\[\\s*\\{.*")) return true; // non-empty array
        if (response.matches("(?i).*\"status\"\\s*:\\s*\"ERROR\".*")) return true;

        return false;
    }

    private static String readAll(InputStream is) {
        if (is == null) return null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
            return baos.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String preview(String s) {
        if (s == null) return "<null>";
        return s.length() <= 200 ? s : s.substring(0, 200);
    }

    private static void setJson(Exchange ex, int code, String json) {
        ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, code);
        ex.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        ex.getIn().setBody(json);
    }

    private static void writeError(Exchange ex, int code, String msg) {
        setJson(ex, code,
                "{ \"status\":\"ERROR\",\"message\":\"" + esc(msg) + "\"," +
                        "\"errorCode\":\"\",\"errorMessage\":\"\",\"details\":{} }");
    }

    private static String rawAsJson(Exchange ex) {
        byte[] csv = ex.getProperty("_csvBytes", byte[].class);
        if (csv == null) return "\"\"";
        String t = new String(csv, StandardCharsets.UTF_8);
        return "\"" + esc(t) + "\"";
    }

    private static String safeJson(String maybeJson) {
        if (maybeJson == null) return "null";
        String t = maybeJson.trim();
        if ((t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))) return t;
        return "\"" + esc(maybeJson) + "\"";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private static void disableSSLVerification() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((h, s) -> true);
        } catch (Exception ignored) {}
    }
}
