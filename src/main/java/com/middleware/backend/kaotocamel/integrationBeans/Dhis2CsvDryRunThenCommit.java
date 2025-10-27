package com.middleware.backend.kaotocamel.integrationBeans;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
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
import java.util.HashMap;
import java.util.Map;

@Component("dhis2CsvDryRunThenCommit")
/**
 * DHIS2 CSV Upload processor that reads config from IntegratedSystemProducer
 * Config is provided in the 'config' header as JSON
 */
public class Dhis2CsvDryRunThenCommit implements Processor {

    private static final Logger log = LoggerFactory.getLogger(Dhis2CsvDryRunThenCommit.class);
    private volatile int lastResponseCode = 0;

    @Override
    public void process(Exchange exchange) {
         String configJson = exchange.getIn().getHeader("config", String.class);

        if (configJson == null || configJson.isEmpty()) {
            writeError(exchange, 400, "DHIS2 config not provided in header 'config'");
            return;
        }

        log.info("Received config JSON: {}", configJson);

        Map<String, String> config;
        try {
             config = parseConfigJson(configJson);
            log.info("Parsed config: host={}, protocol={}, username={}",
                    config.get("host"), config.get("protocol"), config.get("username"));
        } catch (Exception e) {
            log.error("Failed to parse config JSON", e);
            writeError(exchange, 400, "Invalid config JSON format: " + e.getMessage());
            return;
        }

        if (config.isEmpty()) {
            writeError(exchange, 404, "Empty DHIS2 configuration");
            return;
        }

        byte[] csv = exchange.getProperty("_csvBytes", byte[].class);
        String qBase = exchange.getProperty("_queryBase", String.class);
        String dryRun = exchange.getProperty("dryRun", String.class);

        if (csv == null || qBase == null) {
            writeError(exchange, 500, "Missing CSV bytes or query parameters");
            return;
        }

        boolean isDryRunMode = !"false".equalsIgnoreCase(dryRun);

        try {
            if (isDryRunMode) {
                handleDryRun(exchange, csv, qBase, config);
            } else {
                handleDirectCommit(exchange, csv, qBase, config);
            }
        } catch (Exception e) {
            log.error("DHIS2 upload failed", e);
            writeError(exchange, 500, "DHIS2 upload failed: " + e.getMessage());
        }
    }

    /**
     * Parse JSON config string to Map
     * Handles simple JSON format without external libraries
     */
    private Map<String, String> parseConfigJson(String json) throws Exception {
        Map<String, String> map = new HashMap<>();

        // Simple JSON parsing
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format");
        }

        // Remove braces
        json = json.substring(1, json.length() - 1);

        // Split by comma (handle quotes properly)
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                String value = keyValue[1].trim().replaceAll("^\"|\"$", "");

                // تجاهل null values
                if (!value.equals("null") && !value.isEmpty()) {
                    map.put(key, value);
                }
            }
        }

        return map;
    }

    private void handleDryRun(Exchange exchange, byte[] csv, String qBase, Map<String, String> config) throws Exception {
        log.info("=== DRY RUN phase ===");

        String dryResp = callDhis2(csv, qBase, true, config);

        if (checkConflictsYamlLogic(dryResp, getLastResponseCode())) {
            setJson(exchange, 409, "{ \"status\":\"CONFLICT\",\"message\":\"Resolve conflicts then re-upload\",\"details\":" + safeJson(dryResp) + "}");
            exchange.setRouteStop(true);
            return;
        }

        log.info("=== COMMIT phase ===");

        String commitResp = callDhis2(csv, qBase, false, config);
        int commitCode = getLastResponseCode();

        String status = (commitCode >= 200 && commitCode <= 202) ? "SUCCESS" : "ERROR";
        setJson(exchange, commitCode, "{ \"status\":\"" + status + "\",\"message\":\"Import completed successfully\",\"details\":" + safeJson(commitResp) + " }");
    }

    private void handleDirectCommit(Exchange exchange, byte[] csv, String qBase, Map<String, String> config) throws Exception {
        log.info("=== Direct commit ===");

        String resp = callDhis2(csv, qBase, false, config);
        int code = getLastResponseCode();

        String status = (code == 409) ? "CONFLICT" : (code >= 200 && code <= 202) ? "SUCCESS" : "ERROR";
        setJson(exchange, code, "{ \"status\":\"" + status + "\",\"message\":\"Import completed\",\"details\":" + safeJson(resp) + " }");
    }

    private String buildBaseUrl(Map<String, String> config) {
        String protocol = config.getOrDefault("protocol", "https").toLowerCase();
        String host = config.get("host");
        String port = config.get("port");

        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("DHIS2 host not found in config");
        }

        String baseUrl;
        if (host.startsWith("http")) {
            baseUrl = host;
        } else {
            baseUrl = protocol + "://" + host;
             if (port != null && !port.isEmpty()
                    && !port.equalsIgnoreCase("HTTP")
                    && !port.equalsIgnoreCase("HTTPS")
                    && !port.equals("80")
                    && !port.equals("443")) {
                baseUrl += ":" + port;
            }
        }
        return baseUrl;
    }

    private String callDhis2(byte[] csv, String qBase, boolean dryRun, Map<String, String> config) throws Exception {
        String baseUrl = buildBaseUrl(config);
        String fullUrl = baseUrl + "/api/dataValueSets?dryRun=" + (dryRun ? "true" : "false");
        if (qBase != null && !qBase.isEmpty()) {
            fullUrl += "&" + qBase;
        }

        String user = config.getOrDefault("username", "");
        String pass = config.getOrDefault("password", "");

        log.info("📤 Calling DHIS2: {}", fullUrl);

        disableSSLVerification();

        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String auth = "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", auth);
        conn.setRequestProperty("Content-Type", "application/csv");
        conn.setRequestProperty("Accept", "application/json");

        conn.setConnectTimeout(30000);
        conn.setReadTimeout(180000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(csv);
            os.flush();
        }

        lastResponseCode = conn.getResponseCode();
        InputStream is = (lastResponseCode >= 200 && lastResponseCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        return readAll(is);
    }

    private int getLastResponseCode() {
        return lastResponseCode;
    }

    private static boolean checkConflictsYamlLogic(String response, int responseCode) {
        if (responseCode == 409) return true;
        if (response == null) return false;
        if (response.contains("\"httpStatus\":\"Conflict\"")) return true;
        if (response.matches("(?is).*\"conflicts\"\\s*:\\s*\\[\\s*\\{.*")) return true;
        if (response.contains("\"status\":\"ERROR\"")) return true;
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

    private static void setJson(Exchange ex, int code, String json) {
        ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, code);
        ex.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        ex.getIn().setBody(json);
    }

    private static void writeError(Exchange ex, int code, String msg) {
        setJson(ex, code, "{ \"status\":\"ERROR\",\"message\":\"" + esc(msg) + "\"}");
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