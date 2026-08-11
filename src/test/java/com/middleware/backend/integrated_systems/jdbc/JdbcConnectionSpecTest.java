package com.middleware.backend.integrated_systems.jdbc;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcConnectionSpecTest {

    @Test
    void buildsPostgresUrlWithSslMode() {
        Map<String, Object> config = base("POSTGRESQL");
        config.put("databaseName", "neondb");
        config.put("sslMode", "require");

        JdbcConnectionSpec spec = JdbcConnectionSpec.fromConfig("NEON.TEST", config);

        assertEquals(
                "jdbc:postgresql://ep.example:5432/neondb?sslmode=require",
                spec.getJdbcUrl());
        assertEquals("org.postgresql.Driver", spec.getDriverClass());
    }

    @Test
    void buildsMysqlUrl() {
        Map<String, Object> config = base("MYSQL");
        config.put("database", "ems");
        config.put("port", "3306");

        JdbcConnectionSpec spec = JdbcConnectionSpec.fromConfig("MYSQL_DB", config);

        assertEquals("jdbc:mysql://ep.example:3306/ems", spec.getJdbcUrl());
        assertEquals("com.mysql.cj.jdbc.Driver", spec.getDriverClass());
    }

    @Test
    void buildsSqlServerUrl() {
        Map<String, Object> config = base("SQLSERVER");
        config.put("database", "app");
        config.put("port", "1433");

        JdbcConnectionSpec spec = JdbcConnectionSpec.fromConfig("MSSQL", config);

        assertEquals("jdbc:sqlserver://ep.example:1433;databaseName=app", spec.getJdbcUrl());
    }

    @Test
    void buildsOracleUrl() {
        Map<String, Object> config = base("ORACLE");
        config.put("database", "ORCL");
        config.put("port", "1521");

        JdbcConnectionSpec spec = JdbcConnectionSpec.fromConfig("ORA", config);

        assertEquals("jdbc:oracle:thin:@//ep.example:1521/ORCL", spec.getJdbcUrl());
    }

    @Test
    void rejectsUnsupportedProtocol() {
        Map<String, Object> config = base("HTTP");
        config.put("database", "x");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> JdbcConnectionSpec.fromConfig("BAD", config));
        assertTrue(ex.getMessage().contains("Unsupported database protocol"));
    }

    @Test
    void rejectsMissingDatabase() {
        Map<String, Object> config = base("POSTGRESQL");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> JdbcConnectionSpec.fromConfig("BAD", config));
        assertTrue(ex.getMessage().contains("database"));
    }

    private static Map<String, Object> base(String protocol) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("systemType", "DATABASE");
        config.put("protocol", protocol);
        config.put("host", "ep.example");
        config.put("port", "5432");
        config.put("username", "user");
        config.put("password", "secret");
        return config;
    }
}
