//package com.middleware.backend.kaotocamel.config;
//
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class CamelSecurityInterceptor extends RouteBuilder {
//
//    private final RouteAuthProcessor routeAuthProcessor;
//
//    public CamelSecurityInterceptor(RouteAuthProcessor routeAuthProcessor) {
//        this.routeAuthProcessor = routeAuthProcessor;
//    }
//    @Override
//    public void configure() {
//        // Intercept all REST DSL routes
//        interceptFrom("rest:*")
//                .process(exchange -> {
//                    System.out.println("Intercepted route: " + exchange.getFromRouteId());
//                    routeAuthProcessor.process(exchange);
//                });
//    }
//}
