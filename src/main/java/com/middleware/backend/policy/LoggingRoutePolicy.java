package com.middleware.backend.policy;

import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.support.RoutePolicySupport;

import com.middleware.backend.logging.service.MiddlewareApiCallLogService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingRoutePolicy extends RoutePolicySupport {

    private final String routeId;
    private final MiddlewareApiCallLogService logService;

    public LoggingRoutePolicy(String routeId, MiddlewareApiCallLogService logService) {
        this.routeId = routeId;
        this.logService = logService;
    }


    @Override
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
    public void onExchangeDone(Route route, Exchange exchange) {
        Long apiLogId = exchange.getProperty("apiLogId", Long.class);
        if (apiLogId != null && apiLogId > 0) {
            logService.updateTransaction(exchange);
        } else {
            log.debug("LoggingRoutePolicy.onExchangeDone: Route [{}] - No apiLogId found, skipping update", routeId);
        }
    }
}
