package com.middleware.backend.policy;

import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.support.RoutePolicySupport;

import com.middleware.backend.logging.service.MiddlewareApiCallLogService;

import lombok.extern.slf4j.Slf4j;

/**
 * Camel RoutePolicy that logs and persists API call transactions for a route.
 * <p>
 * On exchange begin, it creates a transaction record via
 * {@link com.middleware.backend.logging.service.MiddlewareApiCallLogService}
 * and stores the generated id on the exchange under the key {@code apiLogId}.
 * On exchange completion, it updates the corresponding transaction using the
 * same log service.
 */
@Slf4j
public class LoggingRoutePolicy extends RoutePolicySupport {

    private final String routeId;
    private final MiddlewareApiCallLogService logService;

    /**
     * Creates a new logging route policy bound to a specific route.
     *
     * @param routeId    the Camel route id this policy is associated with
     * @param logService service used to persist and update API call logs
     */
    public LoggingRoutePolicy(String routeId, MiddlewareApiCallLogService logService) {
        this.routeId = routeId;
        this.logService = logService;
    }


    @Override
    /**
     * Invoked when an exchange enters the route. Attempts to create a
     * transaction log and stores its id in the exchange properties.
     * If validation fails, the route is stopped for this exchange.
     *
     * @param route    the current route
     * @param exchange the incoming exchange
     */
    public void onExchangeBegin(Route route, Exchange exchange) {
        log.info("LoggingRoutePolicy.onExchangeBegin: Route [{}] - Incoming exchange: {}", routeId, exchange.getExchangeId());
        try {
            Long apiLogId = logService.createTransactionSync(routeId, exchange);
            if (apiLogId != null && apiLogId > 0) {
                exchange.setProperty("apiLogId", apiLogId);
                log.info("LoggingRoutePolicy.onExchangeBegin: Route [{}] - call log created with ID {}", routeId, apiLogId);
            } else {
                log.info("LoggingRoutePolicy.onExchangeBegin: Route [{}] - Skipping log creation (dry-run or disabled)", routeId);
            }
        } catch (IllegalArgumentException e) {
            log.error("LoggingRoutePolicy.onExchangeBegin: Route [{}] - Validation failed: {}", routeId, e.getMessage());
            exchange.setRouteStop(true);
        }
    }

    @Override
    /**
     * Invoked when an exchange completes. If an {@code apiLogId} property is
     * present, the corresponding transaction is updated via the log service.
     *
     * @param route    the current route
     * @param exchange the completed exchange
     */
    public void onExchangeDone(Route route, Exchange exchange) {
        Long apiLogId = exchange.getProperty("apiLogId", Long.class);
        if (apiLogId != null && apiLogId > 0) {
            logService.updateTransaction(exchange);
        } else {
            log.debug("LoggingRoutePolicy.onExchangeDone: Route [{}] - No apiLogId found, skipping update", routeId);
        }
    }
}
