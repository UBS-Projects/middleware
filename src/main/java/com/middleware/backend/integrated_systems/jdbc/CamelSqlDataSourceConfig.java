package com.middleware.backend.integrated_systems.jdbc;

import org.apache.camel.CamelContext;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Points Camel's {@code sql} component at an {@link ExchangeRoutingDataSource}
 * <strong>before</strong> routes start, so {@code integratedSystem} (DATABASE) can
 * switch the JDBC target for the current exchange without changing Kaoto routes.
 * <p>
 * JPA keeps using the primary {@code dataSource} bean unchanged.
 */
@Configuration
public class CamelSqlDataSourceConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CamelSqlDataSourceConfig.class);

    private final DataSource primaryDataSource;

    public CamelSqlDataSourceConfig(@Qualifier("dataSource") DataSource primaryDataSource) {
        this.primaryDataSource = primaryDataSource;
    }

    @Bean
    public CamelContextConfiguration integratedSystemSqlDataSourceConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(CamelContext camelContext) {
                wireSqlComponent(camelContext);
            }

            @Override
            public void afterApplicationStart(CamelContext camelContext) {
                // no-op
            }
        };
    }

    private void wireSqlComponent(CamelContext camelContext) {
        SqlComponent sqlComponent = camelContext.getComponent("sql", SqlComponent.class);
        if (sqlComponent == null) {
            LOG.warn("Camel sql component not found — Integrated System DATABASE routing will not work");
            return;
        }

        // Avoid double-wrapping on refresh/restart
        if (sqlComponent.getDataSource() instanceof ExchangeRoutingDataSource) {
            LOG.debug("Camel sql component already uses ExchangeRoutingDataSource");
            return;
        }

        DataSource fallback = sqlComponent.getDataSource() != null
                ? sqlComponent.getDataSource()
                : primaryDataSource;

        sqlComponent.setDataSource(new ExchangeRoutingDataSource(fallback));
        LOG.info(
                "Camel sql component DataSource set to ExchangeRoutingDataSource "
                        + "(Integrated System DB when bound by integratedSystem, else local primary DataSource)");
    }
}
