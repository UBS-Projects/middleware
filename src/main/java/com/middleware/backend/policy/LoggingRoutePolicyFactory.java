package com.middleware.backend.policy;

import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.springframework.stereotype.Component;

import com.middleware.backend.logging.service.MiddlewareApiCallLogService;

/**
 * Factory for creating {@link LoggingRoutePolicy} instances per Camel route.
 * <p>
 * Injected into the Camel context so each route gets its own policy instance
 * wired with the shared {@code MiddlewareApiCallLogService}.
 */
@Component
public class LoggingRoutePolicyFactory implements RoutePolicyFactory {

    private final MiddlewareApiCallLogService logService;

    /**
     * Constructs the factory with the shared log service dependency.
     *
     * @param logService service used by route policies to persist logs
     */
    public LoggingRoutePolicyFactory(MiddlewareApiCallLogService logService) {
        this.logService = logService;
    }

    @Override
    /**
     * Creates a new {@link LoggingRoutePolicy} instance bound to the given route id.
     *
     * @param context    the Camel context
     * @param routeId    the id of the route being constructed
     * @param definition the route definition node
     * @return a new policy instance
     */
    public RoutePolicy createRoutePolicy(CamelContext context, String routeId, NamedNode definition) {
        return new LoggingRoutePolicy(routeId, logService);
    }
}
