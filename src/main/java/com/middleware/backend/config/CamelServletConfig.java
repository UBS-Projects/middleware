package com.middleware.backend.config;

import org.apache.camel.Configuration;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@Configuration
public class CamelServletConfig {

    @Bean
    ServletRegistrationBean<CamelHttpTransportServlet> camelServlet() {
        ServletRegistrationBean<CamelHttpTransportServlet> servlet = new ServletRegistrationBean<>(
                new CamelHttpTransportServlet(), "/camel/*");
        servlet.setName("CamelServlet");
        return servlet;

    }

}
