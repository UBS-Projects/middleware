package com.middleware.backend.config;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.language.bean.Bean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Camel HTTP transport servlet.
 * This class is responsible for registering the {@link CamelHttpTransportServlet}
 * to handle Camel routes exposed over HTTP.
 */
@Configuration
public class CamelServletConfig {

    /**
     * Creates and registers the {@link CamelHttpTransportServlet}.
     * The servlet is mapped to handle requests under the "/camel/*" URL pattern.
     *
     * @return A {@link ServletRegistrationBean} for the Camel HTTP transport servlet.
     */
    @Bean(ref = "")
    ServletRegistrationBean<CamelHttpTransportServlet> camelServlet() {
        ServletRegistrationBean<CamelHttpTransportServlet> servlet = new ServletRegistrationBean<>(
                new CamelHttpTransportServlet(), "/camel/*");
        servlet.setName("CamelServlet");
        return servlet;

    }
}
