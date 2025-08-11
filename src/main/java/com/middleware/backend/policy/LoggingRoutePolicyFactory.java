package com.middleware.backend.policy;

import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.springframework.stereotype.Component;

import com.middleware.backend.logging.service.MiddlewareApiCallLogService;

@Component
public class LoggingRoutePolicyFactory implements RoutePolicyFactory {

    private final MiddlewareApiCallLogService logService;

    public LoggingRoutePolicyFactory(MiddlewareApiCallLogService logService) {
        this.logService = logService;
    }

    @Override
    public RoutePolicy createRoutePolicy(CamelContext context, String routeId, NamedNode definition) {
        return new LoggingRoutePolicy(routeId, logService);
    }
}
