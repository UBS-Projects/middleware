package com.middleware.backend.config;

import org.apache.camel.Configuration;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.language.bean.Bean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

@Configuration
public class CamelServletConfig {

    @Bean(ref = "")
    ServletRegistrationBean<CamelHttpTransportServlet> camelServlet() {
        ServletRegistrationBean<CamelHttpTransportServlet> servlet = new ServletRegistrationBean<>(
                new CamelHttpTransportServlet(), "/camel/*");
        servlet.setName("CamelServlet");
        return servlet;
    }
}
