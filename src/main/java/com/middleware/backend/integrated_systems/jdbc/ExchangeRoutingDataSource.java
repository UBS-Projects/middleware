package com.middleware.backend.integrated_systems.jdbc;

import com.middleware.sql.IntegratedSystemSqlContext;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * DataSource used by Camel's {@code sql} component.
 * <p>
 * If the current exchange bound an Integrated System DATABASE DataSource
 * (via {@link IntegratedSystemSqlContext}), that connection is used;
 * otherwise the application's primary (local) DataSource is used.
 * <p>
 * This bean is <strong>not</strong> the JPA/Hibernate DataSource — only Camel SQL
 * is rewired to it so middleware tables stay on the local DB.
 */
public class ExchangeRoutingDataSource implements DataSource {

    private final DataSource fallback;

    public ExchangeRoutingDataSource(DataSource fallback) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    private DataSource target() {
        DataSource bound = IntegratedSystemSqlContext.current();
        return bound != null ? bound : fallback;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return target().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return target().getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return target().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        target().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        target().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return target().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return target().getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return target().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || target().isWrapperFor(iface);
    }
}
