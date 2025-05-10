package com.middleware.backend.orchestration;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ServiceStepRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:serviceStep")
            .routeId("service-step")
            .log("Executing service step: ${body.name}")
            .bean("serviceStepProcessor", "process(${body})");
    }
}
