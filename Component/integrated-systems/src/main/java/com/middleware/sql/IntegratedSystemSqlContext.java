package com.middleware.sql;

import org.apache.camel.Exchange;
import org.apache.camel.support.SynchronizationAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Binds a per-exchange JDBC {@link DataSource} so Camel's {@code sql} component
 * (wired with {@code ExchangeRoutingDataSource}) can use it for the rest of the route.
 * <p>
 * Uses a ThreadLocal for the current worker thread and clears it on exchange completion
 * (success or failure) so pooled threads never leak a foreign DataSource.
 */
public final class IntegratedSystemSqlContext {

    public static final String EXCHANGE_PROPERTY = "IntegratedSystemDataSource";
    public static final String HEADER_CAMEL_SQL_DATASOURCE = "CamelSqlDataSource";

    private static final Logger LOG = LoggerFactory.getLogger(IntegratedSystemSqlContext.class);
    private static final ThreadLocal<DataSource> CURRENT = new ThreadLocal<>();

    private IntegratedSystemSqlContext() {
    }

    public static DataSource current() {
        return CURRENT.get();
    }

    /**
     * Bind {@code dataSource} for this exchange and the current thread.
     * Registers an on-completion callback to clear the ThreadLocal.
     */
    public static void bind(Exchange exchange, DataSource dataSource) {
        if (exchange == null || dataSource == null) {
            return;
        }

        CURRENT.set(dataSource);
        exchange.setProperty(EXCHANGE_PROPERTY, dataSource);
        // Camel 4.8+ sql producer also honors this header; harmless on 4.4.
        exchange.getIn().setHeader(HEADER_CAMEL_SQL_DATASOURCE, dataSource);

        exchange.getExchangeExtension().addOnCompletion(new SynchronizationAdapter() {
            @Override
            public void onComplete(Exchange exchange) {
                clear();
            }

            @Override
            public void onFailure(Exchange exchange) {
                clear();
            }
        });

        LOG.debug("Bound IntegratedSystem DataSource for exchange {}", exchange.getExchangeId());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
