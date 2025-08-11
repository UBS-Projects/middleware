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

        log.info("LoggingRoutePolicy.onExchangeBegin: Route [{}] - Incoming exchange: {}", routeId,
                exchange.getExchangeId());

        long id = logService.createTransactionSync(routeId, exchange);
        exchange.setProperty("apiLogId", id); // store ID for later use
        log.info("LoggingRoutePolicy.onExchangeBegin: Route [{}] - call log created with for excahnage content {}",
                routeId, exchange);

    }

    @Override
    public void onExchangeDone(Route route, Exchange exchange) {

        logService.updateTransaction(exchange);

    }

}
