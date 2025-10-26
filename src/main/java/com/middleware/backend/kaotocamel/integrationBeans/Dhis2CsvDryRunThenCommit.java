package com.middleware.backend.kaotocamel.integrationBeans;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
import java.util.Map;

@Component("dhis2CsvDryRunThenCommit")
public class Dhis2CsvDryRunThenCommit implements Processor {

    private static final Logger log = LoggerFactory.getLogger(Dhis2CsvDryRunThenCommit.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private volatile int lastResponseCode = 0;

    @Override
    public void process(Exchange exchange) {
        try {
            // ✅ Fetch DHIS2 config JSON from header
            String jsonConfig = exchange.getIn().getHeader("config", String.class);

            if (jsonConfig == null || jsonConfig.isEmpty()) {
                writeError(exchange, 400, "Missing DHIS2 configuration in header 'dhis2'");
                return;
            }

            // ✅ Parse config JSON into a Map
            Map<String, String> config = mapper.readValue(jsonConfig, new TypeReference<Map<String, String>>() {});

            byte[] csv = exchange.getProperty("_csvBytes", byte[].class);
            String qBase = exchange.getProperty("_queryBase", String.class);
            String dryRun = exchange.getProperty("dryRun", String.class);

            if (csv == null || qBase == null) {
                writeError(exchange, 500, "Missing CSV bytes or query parameters");
                return;
            }

            boolean isDryRunMode = !"false".equalsIgnoreCase(dryRun);

            if (isDryRunMode) {
                handleDryRun(exchange, csv, qBase, config);
            } else {
                handleDirectCommit(exchange, csv, qBase, config);
            }

        } catch (Exception e) {
            writeError(exchange, 500, "DHIS2 upload failed: " + e.getMessage());
        }
    }

    // ✅ SAME as before
    private void handleDryRun(Exchange exchange, byte[] csv, String qBase, Map<String, String> config) throws Exception {
        String dryResp = callDhis2(csv, qBase, true, config);
        int dryCode = getLastResponseCode();

        if (dryResp == null || dryCode == 0) {
            writeError(exchange, 500, "DHIS2 unreachable during dry-run");
            return;
        }

        if (checkConflictsYamlLogic(dryResp, dryCode)) {
            setJson(exchange, 409,
                    "{ \"status\":\"CONFLICT\",\"message\":\"Resolve conflicts then re-upload\",\"details\":" + safeJson(dryResp) + "}");
            exchange.setRouteStop(true);
            return;
        }

        String commitResp = callDhis2(csv, qBase, false, config);
        int commitCode = getLastResponseCode();

        String status = (commitCode >= 200 && commitCode <= 202) ? "SUCCESS" : "ERROR";
        setJson(exchange, commitCode,
                "{ \"status\":\"" + status + "\",\"message\":\"completed successfully\",\"details\":" + safeJson(commitResp) + " }");
    }

    private void handleDirectCommit(Exchange exchange, byte[] csv, String qBase, Map<String, String> config) throws Exception {
        String resp = callDhis2(csv, qBase, false, config);
        int codeResp = getLastResponseCode();
        String status = (codeResp == 409) ? "CONFLICT" :
                (codeResp >= 200 && codeResp <= 202) ? "SUCCESS" : "ERROR";
        setJson(exchange, codeResp,
                "{ \"status\":\"" + status + "\",\"message\":\"completed (no dry-run)\",\"details\":" + safeJson(resp) + " }");
    }

    // ✅ same HTTP logic
    private String callDhis2(byte[] csv, String qBase, boolean dryRun, Map<String, String> config) throws Exception {
        String protocol = config.getOrDefault("protocol", "https").toLowerCase();
        String host = config.get("host");
        String port = config.get("port");

        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("DHIS2 host not found in config map");
        }

        String baseUrl = host.startsWith("http")
                ? host
                : protocol + "://" + host;

        String fullUrl = baseUrl + "/api/dataValueSets?dryRun=" + (dryRun ? "true" : "false");
        if (qBase != null && !qBase.isEmpty()) fullUrl += "&" + qBase;

        String user = config.getOrDefault("username", "");
        String pass = config.getOrDefault("password", "");

        disableSSLVerification();

        log.info("Calling DHIS2 {}", fullUrl);
        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String auth = "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", auth);
        conn.setRequestProperty("Content-Type", "application/csv");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Middleware-DHIS2/1.0");

        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(csv);
        }

        lastResponseCode = conn.getResponseCode();
        InputStream is = (lastResponseCode >= 200 && lastResponseCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        return readAll(is);
    }

    // --- Utility methods (unchanged) ---
    private int getLastResponseCode() { return lastResponseCode; }

    private static boolean checkConflictsYamlLogic(String response, int responseCode) {
        if (responseCode == 409) return true;
        if (response == null) return false;
        return response.matches("(?i).*\"httpStatus\"\\s*:\\s*\"Conflict\".*")
                || response.matches("(?is).*\"conflicts\"\\s*:\\s*\\[\\s*\\{.*")
                || response.matches("(?i).*\"status\"\\s*:\\s*\"ERROR\".*");
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
        setJson(ex, code,
                "{ \"status\":\"ERROR\",\"message\":\"" + esc(msg) + "\",\"details\":{} }");
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
