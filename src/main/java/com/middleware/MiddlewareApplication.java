package com.middleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
<<<<<<< HEAD
import org.springframework.context.annotation.ComponentScan;
<<<<<<< HEAD
=======
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
>>>>>>> 8d25c9ec9dfbbc79e7583cbda84cadd91a6a67fb
=======
import org.springframework.scheduling.annotation.EnableAsync;
>>>>>>> origin/changes_branch

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = "com.middleware.backend")
public class MiddlewareApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiddlewareApplication.class, args);

<<<<<<< HEAD


	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
=======
>>>>>>> origin/changes_branch
	}
}
