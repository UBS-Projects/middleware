package com.middleware.backend.kaotocamel.integrationBeans;

import com.middleware.backend.integrated_systems.dto.IntegratedSystemDto;
import com.middleware.backend.integrated_systems.service.IntegratedSystemService;
import com.middleware.backend.system_settings.model.Config;
import com.middleware.backend.system_settings.repository.ConfigRepository;
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
import java.util.List;
import java.util.Map;

@Component("dhis2CsvDryRunThenCommit")
/**
 * Camel Processor that uploads CSV data to DHIS2's dataValueSets API.
 * It performs an optional dry-run first to detect conflicts, then commits;
 * or imports directly when dry-run is disabled. Produces a unified JSON response.
 */
public class Dhis2CsvDryRunThenCommit implements Processor {

    private static final Logger log = LoggerFactory.getLogger(Dhis2CsvDryRunThenCommit.class);
    private final IntegratedSystemService service;
    private volatile int lastResponseCode = 0;
    public Dhis2CsvDryRunThenCommit(IntegratedSystemService service) {
        this.service = service;
    }

    @Override
    /**
     * Executes the DHIS2 CSV upload flow using exchange properties populated by {@code CsvGuard}:
     * - {@code _csvBytes}: raw CSV payload
     * - {@code _queryBase}: base query parameters
     * - {@code dryRun}: "true"/"false" string flag
     * Writes unified JSON body and HTTP status headers on the exchange.
     */
    public void process(Exchange exchange) {
        String code = exchange.getIn().getHeader("_dhis2Code", String.class);
        if (code == null) {
            writeError(exchange, 400, "DHIS2 code not provided in exchange property '_dhis2Code'");
            return;
        }
        // Fetch config dynamically
        Map<String, String> config = getDhis2Config(code);
        if (config.isEmpty()) {
            writeError(exchange, 404, "No DHIS2 configuration found for code: " + code);
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
            writeError(exchange, 500, "DHIS2 upload failed: " + e.getMessage());
        }
    }

    private Map<String, String> getDhis2Config(String code) {
        IntegratedSystemDto dto = service.getById(code);

        if (dto == null) {
            throw new RuntimeException("No DHIS2 config found for code: " + code);
        }

        Map<String, String> map = new HashMap<>();
        map.put("host", dto.getHost());
        map.put("port", String.valueOf(dto.getPort()));
        map.put("protocol", String.valueOf(dto.getProtocol()));
        map.put("authenticationType", String.valueOf(dto.getAuthenticationType()));
        map.put("username", dto.getUsername());
        map.put("password", dto.getPassword());
        map.put("token", dto.getToken());

        // Optional additional keys
        if (dto.getAdditionalKey1() != null && dto.getAdditionalValue1() != null) {
            map.put(dto.getAdditionalKey1(), dto.getAdditionalValue1());
        }
        if (dto.getAdditionalKey2() != null && dto.getAdditionalValue2() != null) {
            map.put(dto.getAdditionalKey2(), dto.getAdditionalValue2());
        }
        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
        System.out.println(map);
        System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
        return map;
    }

    private void handleDryRun(Exchange exchange, byte[] csv, String qBase, Map<String, String> config) throws Exception {
        String dryResp = callDhis2(csv, qBase, true, config);
        int dryCode = getLastResponseCode();

        if (dryResp == null || dryCode == 0) {
            writeError(exchange, 500, "DHIS2 unreachable during dry-run");
            return;
        }

        if (checkConflictsYamlLogic(dryResp, dryCode)) {
            setJson(exchange, 409, "{ \"status\":\"CONFLICT\",\"message\":\"Resolve conflicts then re-upload\",\"details\":" + safeJson(dryResp) + "}");
            exchange.setRouteStop(true);
            return;
        }

        String commitResp = callDhis2(csv, qBase, false, config);
        int commitCode = getLastResponseCode();

        String status = (commitCode >= 200 && commitCode <= 202) ? "SUCCESS" : "ERROR";
        setJson(exchange, commitCode, "{ \"status\":\"" + status + "\",\"message\":\"completed successfully\",\"details\":" + safeJson(commitResp) + " }");
    }

    private void handleDirectCommit(Exchange exchange, byte[] csv, String qBase, Map<String, String> config) throws Exception {

        String resp = callDhis2(csv, qBase, false, config);
        int codeResp = getLastResponseCode();
        String status = (codeResp == 409) ? "CONFLICT" : (codeResp >= 200 && codeResp <= 202) ? "SUCCESS" : "ERROR";
        setJson(exchange, codeResp, "{ \"status\":\"" + status + "\",\"message\":\"completed (no dry-run)\",\"details\":" + safeJson(resp) + " }");
    }


    /**
     * Calls DHIS2 dataValueSets endpoint with the given CSV and query parameters.
     * Returns the response body and stores the HTTP code internally.
     */
    private String callDhis2(byte[] csv, String qBase, boolean dryRun, Map<String, String> config) throws Exception {
        System.out.println("*******************************");
        System.out.println(config);
        System.out.println("*******************************");

        // Build base URL from protocol + host + optional port
        String protocol = config.getOrDefault("protocol", "https").toLowerCase();
        String host = config.get("host");
        String port = config.get("port");

        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("DHIS2 host not found in config map");
        }

        // Construct the base URL
        String baseUrl;
        if (host.startsWith("http")) {
            baseUrl = host;
        } else {
            baseUrl = protocol + "://" + host;
            if (port != null && !port.isEmpty() && !"80".equals(port) && !"443".equals(port)) {
                baseUrl += ":" + port;
            }
        }

        // Build the final DHIS2 endpoint
        String fullUrl = baseUrl + "/api/dataValueSets?dryRun=" + (dryRun ? "true" : "false");
        if (qBase != null && !qBase.isEmpty()) {
            fullUrl += "&" + qBase;
        }

        // Retrieve credentials
        String user = config.getOrDefault("username", "");
        String pass = config.getOrDefault("password", "");

        log.info("Calling DHIS2 {}", fullUrl);

        // Disable SSL validation (use only for testing)
        disableSSLVerification();

        // Open connection
        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        // Add headers
        String auth = "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", auth);
        conn.setRequestProperty("Content-Type", "application/csv");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Middleware-DHIS2/1.0");
        conn.setRequestProperty("Cache-Control", "no-cache");

        // Timeouts
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        // Send CSV payload
        try (OutputStream os = conn.getOutputStream()) {
            os.write(csv);
            os.flush();
        }

        // Read response
        lastResponseCode = conn.getResponseCode();
        InputStream is = (lastResponseCode >= 200 && lastResponseCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        return readAll(is);
    }



    private int getLastResponseCode() { return lastResponseCode; }

    private static int safeCode(HttpURLConnection c) {
        try { return c.getResponseCode(); }
        catch (Exception e) { return 0; }
    }

    /**
     * Detects conflict conditions in DHIS2 responses using status code and JSON patterns.
     */
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
