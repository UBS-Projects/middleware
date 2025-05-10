package com.middleware.backend.orchestration;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class MapperStepRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:mapperStep")
            .routeId("mapper-step")
            .log("Executing mapper step: ${body.name}")
            .bean("mapperStepProcessor", "process(${body})");
    }
}
