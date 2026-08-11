package com.middleware.backend.integrated_systems.jdbc;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Builds JDBC URLs and resolves driver class names for DATABASE integrated systems.
 */
public final class JdbcConnectionSpec {

    public enum DbProtocol {
        POSTGRESQL("org.postgresql.Driver"),
        MYSQL("com.mysql.cj.jdbc.Driver"),
        SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver"),
        ORACLE("oracle.jdbc.OracleDriver");

        private final String driverClass;

        DbProtocol(String driverClass) {
            this.driverClass = driverClass;
        }

        public String driverClass() {
            return driverClass;
        }

        public static DbProtocol from(String protocol) {
            if (protocol == null || protocol.isBlank()) {
                throw new IllegalArgumentException("Unsupported database protocol: null");
            }
            String normalized = protocol.trim().toUpperCase(Locale.ROOT)
                    .replace("-", "")
                    .replace(" ", "");
            // common aliases
            if ("POSTGRES".equals(normalized) || "PG".equals(normalized)) {
                normalized = "POSTGRESQL";
            }
            if ("MSSQL".equals(normalized) || "SQL_SERVER".equals(normalized)) {
                normalized = "SQLSERVER";
            }
            try {
                return DbProtocol.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unsupported database protocol: " + protocol);
            }
        }
    }

    private final String code;
    private final DbProtocol protocol;
    private final String host;
    private final String port;
    private final String database;
    private final String username;
    private final String password;
    private final String sslMode;
    private final String jdbcUrl;
    private final String fingerprint;

    private JdbcConnectionSpec(
            String code,
            DbProtocol protocol,
            String host,
            String port,
            String database,
            String username,
            String password,
            String sslMode,
            String jdbcUrl) {
        this.code = code;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.sslMode = sslMode;
        this.jdbcUrl = jdbcUrl;
        this.fingerprint = String.join(
                "|",
                code == null ? "" : code,
                protocol.name(),
                host,
                port,
                database,
                username,
                password,
                sslMode == null ? "" : sslMode,
                jdbcUrl);
    }

    public static JdbcConnectionSpec fromConfig(String code, Map<String, Object> config) {
        Objects.requireNonNull(config, "config");

        String protocolRaw = required(config, "protocol");
        DbProtocol protocol = DbProtocol.from(protocolRaw);

        String host = required(config, "host");
        String port = required(config, "port");
        String database = resolveDatabase(config);
        String username = required(config, "username");
        String password = required(config, "password");
        String sslMode = firstNonBlank(
                asString(config.get("sslMode")),
                asString(config.get("sslmode")));

        String jdbcUrl = buildJdbcUrl(protocol, host, port, database, sslMode);
        return new JdbcConnectionSpec(code, protocol, host, port, database, username, password, sslMode, jdbcUrl);
    }

    private static String buildJdbcUrl(
            DbProtocol protocol, String host, String port, String database, String sslMode) {
        return switch (protocol) {
            case POSTGRESQL -> {
                String base = "jdbc:postgresql://" + host + ":" + port + "/" + database;
                yield appendQueryParam(base, "sslmode", sslMode);
            }
            case MYSQL -> {
                String base = "jdbc:mysql://" + host + ":" + port + "/" + database;
                // sslMode for MySQL is typically sslMode=REQUIRED etc.; pass through if provided
                yield appendQueryParam(base, "sslMode", sslMode);
            }
            case SQLSERVER -> "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + database;
            case ORACLE -> "jdbc:oracle:thin:@//" + host + ":" + port + "/" + database;
        };
    }

    private static String appendQueryParam(String url, String key, String value) {
        if (value == null || value.isBlank()) {
            return url;
        }
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + key + "=" + value.trim();
    }

    private static String resolveDatabase(Map<String, Object> config) {
        String database = firstNonBlank(
                asString(config.get("database")),
                asString(config.get("databaseName")),
                asString(config.get("db")),
                asString(config.get("schema")));
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing required Integrated System field: database (or databaseName)");
        }
        return database.trim();
    }

    private static String required(Map<String, Object> config, String key) {
        String value = asString(config.get(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required Integrated System field: " + key);
        }
        return value.trim();
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    public String getCode() {
        return code;
    }

    public DbProtocol getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSslMode() {
        return sslMode;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getDriverClass() {
        return protocol.driverClass();
    }
}
