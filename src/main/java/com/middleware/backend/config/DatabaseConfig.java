package com.middleware.backend.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Configures the database connection for the application.
 * This class is responsible for creating a {@link DataSource} bean, which provides
 * a connection to the database using properties defined in the application's configuration files.
 */
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    //@Value("${spring.datasource.driver-class-name:oracle.jdbc.OracleDriver}")
    private String dbDriver;

    /**
     * Creates and configures the main {@link DataSource} for the application.
     * The data source is configured with the URL, username, password, and driver class name
     * obtained from the application's properties.
     *
     * @return A configured {@link DriverManagerDataSource} instance.
     */
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(dbDriver);
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);
        return dataSource;
    }
}
