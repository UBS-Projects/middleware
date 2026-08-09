package com.middleware.backend.errormapping.service;

import com.middleware.backend.errormapping.dto.ErrorMappingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.*;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.support.RoutePolicySupport;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Registers a RoutePolicy on every Camel route automatically.
 * When a route fails and has no explicit error-mapping component, this policy
 * looks up a matching global error mapping (route_id='*', source_system_id=NULL)
 * and applies it to the exchange before the HTTP response is sent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalErrorMappingRoutePolicyFactory implements RoutePolicyFactory {

    private final BackendErrorMappingService errorMappingService;

    @Override
    public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode route) {
        return new RoutePolicySupport() {
            @Override
            public void onExchangeDone(Route route, Exchange exchange) {
                // Skip if already handled by the explicit error-mapping component
                Boolean alreadyHandled = exchange.getProperty("CamelErrorMappingHandled", Boolean.class);
                if (Boolean.TRUE.equals(alreadyHandled)) return;

                boolean isFailed = exchange.isFailed();
                Integer httpCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
                boolean isHttpError = httpCode != null && httpCode >= 400;

                // Handle both: real exceptions AND HTTP errors from throwExceptionOnFailure=false
                if (!isFailed && !isHttpError) return;

                String errorMessage;
                Exception caughtEx = null;

                if (isFailed) {
                    caughtEx = exchange.getException();
                    if (caughtEx == null) {
                        caughtEx = exchange.getProperty(ExchangePropertyKey.EXCEPTION_CAUGHT, Exception.class);
                    }
                    errorMessage = caughtEx != null && caughtEx.getMessage() != null
                            ? caughtEx.getMessage()
                            : (caughtEx != null ? caughtEx.getClass().getSimpleName() : "unknown error");
                } else {
                    // No exception — error is in the response body (throwExceptionOnFailure=false)
                    try {
                        errorMessage = exchange.getMessage().getBody(String.class);
                    } catch (Exception e) {
                        errorMessage = null;
                    }
                    if (errorMessage == null || errorMessage.isBlank()) {
                        errorMessage = "HTTP " + httpCode;
                    }
                }

                String camelRouteId = route.getRouteId();
                Long sourceSystemId = exchange.getMessage().getHeader("sourceSystemId", Long.class);

                log.debug("Global error mapping check — routeId: '{}', httpCode: {}, error: '{}'",
                        camelRouteId, httpCode,
                        errorMessage.length() > 300 ? errorMessage.substring(0, 300) + "..." : errorMessage);

                Optional<ErrorMappingDto> mapping;
                try {
                    mapping = errorMappingService.findMatchingErrorMapping(
                            camelRouteId, sourceSystemId, errorMessage);
                } catch (Exception lookupEx) {
                    log.warn("Error during global error mapping lookup: {}", lookupEx.getMessage());
                    return;
                }

                if (mapping.isEmpty()) return;

                ErrorMappingDto dto = mapping.get();
                log.info("Global error mapping applied — code: '{}', status: {}, routeId: '{}'",
                        dto.getMappedErrorCode(), dto.getHttpStatusCode(), camelRouteId);

                if (caughtEx != null) {
                    exchange.setException(null);
                    exchange.removeProperty(ExchangePropertyKey.EXCEPTION_CAUGHT);
                    exchange.setProperty("CamelFailureHandled", Boolean.TRUE);
                }

                Message msg = exchange.getMessage();
                msg.setBody(buildErrorBody(dto));
                msg.setHeader(Exchange.HTTP_RESPONSE_CODE, dto.getHttpStatusCode());
                msg.setHeader(Exchange.CONTENT_TYPE, "application/json");
                exchange.setProperty("CamelErrorMappingHandled", Boolean.TRUE);
            }
        };
    }

    private String buildErrorBody(ErrorMappingDto dto) {
        String message = dto.getMappedMessage() != null
                ? dto.getMappedMessage().replace("\\", "\\\\").replace("\"", "\\\"")
                : "";
        String code = dto.getMappedErrorCode() != null ? dto.getMappedErrorCode() : "UNKNOWN_ERROR";
        return String.format(
                "{\"timestamp\":\"%s\",\"error\":\"%s\",\"message\":\"%s\",\"status\":%d}",
                LocalDateTime.now(), code, message, dto.getHttpStatusCode());
    }
}
