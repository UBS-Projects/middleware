package com.middleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"com.middleware", "com.middleware.backend"})
@EnableAsync
public class MiddlewareApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiddlewareApplication.class, args);
	}

	/*
	 * @Bean public org.apache.camel.spi.CamelContextCustomizer
	 * customizer(RoutePolicyFactory policyFactory) { return camelContext ->
	 * camelContext.addRoutePolicyFactory(policyFactory); }
	 */
}
