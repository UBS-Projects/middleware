package com.middleware.backend.integrated_systems.jdbc;

import com.middleware.model.IntegratedSystemDetail;
import com.middleware.service.IntegratedSystemDataSourceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache of JDBC DataSources keyed by Integrated System code.
 * <p>
 * If configuration for a code changes (different fingerprint), the old pool is closed
 * and replaced so stale credentials are not reused.
 */
@Service(IntegratedSystemDataSourceService.BEAN_ID)
public class IntegratedSystemDataSourceServiceImpl implements IntegratedSystemDataSourceService {

    private static final Logger LOG = LoggerFactory.getLogger(IntegratedSystemDataSourceServiceImpl.class);

    private final ConcurrentHashMap<String, CachedDataSource> cache = new ConcurrentHashMap<>();

    @Override
    public DataSource resolveDataSource(String code, IntegratedSystemDetail detail) {
        Objects.requireNonNull(detail, "detail");
        Map<String, Object> config = detail.getConfig();
        if (config == null) {
            throw new IllegalArgumentException("Integrated System '" + code + "' has no config map");
        }

        Object systemType = config.get("systemType");
        if (systemType == null || !"DATABASE".equalsIgnoreCase(systemType.toString().trim())) {
            throw new IllegalArgumentException(
                    "Integrated System '" + code + "' is not a DATABASE system (systemType=" + systemType + ")");
        }

        JdbcConnectionSpec spec = JdbcConnectionSpec.fromConfig(code, config);
        String cacheKey = code == null ? spec.getFingerprint() : code;

        CachedDataSource cached = cache.compute(cacheKey, (key, existing) -> {
            if (existing != null && existing.fingerprint.equals(spec.getFingerprint())) {
                return existing;
            }
            if (existing != null) {
                LOG.info(
                        "Integrated System '{}' config changed — recreating JDBC DataSource pool",
                        code);
                closeQuietly(existing.dataSource);
            }
            HikariDataSource created = createPool(spec);
            LOG.info(
                    "Created JDBC DataSource for Integrated System '{}' -> {} (driver={})",
                    code,
                    spec.getJdbcUrl(),
                    spec.getDriverClass());
            return new CachedDataSource(spec.getFingerprint(), created);
        });

        return cached.dataSource;
    }

    private static HikariDataSource createPool(JdbcConnectionSpec spec) {
        try {
            Class.forName(spec.getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "JDBC driver not found on classpath: "
                            + spec.getDriverClass()
                            + " (protocol="
                            + spec.getProtocol()
                            + "). Add the corresponding Maven dependency.",
                    e);
        }

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("is-ds-" + sanitize(spec.getCode()));
        hikari.setJdbcUrl(spec.getJdbcUrl());
        hikari.setUsername(spec.getUsername());
        hikari.setPassword(spec.getPassword());
        hikari.setDriverClassName(spec.getDriverClass());
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(0);
        hikari.setIdleTimeout(60_000);
        hikari.setMaxLifetime(300_000);
        hikari.setConnectionTimeout(30_000);
        hikari.setInitializationFailTimeout(-1);
        return new HikariDataSource(hikari);
    }

    private static String sanitize(String code) {
        if (code == null || code.isBlank()) {
            return "unknown";
        }
        return code.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static void closeQuietly(HikariDataSource ds) {
        try {
            if (ds != null && !ds.isClosed()) {
                ds.close();
            }
        } catch (Exception e) {
            LOG.warn("Error closing stale Integrated System DataSource: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        cache.forEach((key, value) -> closeQuietly(value.dataSource));
        cache.clear();
    }

    private record CachedDataSource(String fingerprint, HikariDataSource dataSource) {
    }
}
