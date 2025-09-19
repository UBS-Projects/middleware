package com.middleware.backend.throttling.config;
import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;
import com.middleware.backend.kaotocamel.service.DynamicRouteService;
import com.middleware.backend.throttling.model.CamelLimitConfig;
import com.middleware.backend.throttling.model.CustomCamelLimitConfig;
import com.middleware.backend.throttling.model.RateLimitConfig;
import com.middleware.backend.throttling.service.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.camel.util.AntPathMatcher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servlet {@link Filter} that enforces HTTP request rate limits for the application.
 * <p>
 * Behavior:
 * <ul>
 *   <li>For URIs under <code>/camel/**</code>: applies a global Camel routes limit based on
 *   {@link com.middleware.backend.throttling.model.CamelLimitConfig} and, if configured, a per-route
 *   limit based on {@link com.middleware.backend.throttling.model.CustomCamelLimitConfig} matched
 *   against active routes from {@link com.middleware.backend.kaotocamel.service.DynamicRouteService}.</li>
 *   <li>For all other URIs: applies a global system rate limit from
 *   {@link com.middleware.backend.throttling.model.RateLimitConfig}.</li>
 * </ul>
 * Thread-safety is achieved via {@link java.util.concurrent.atomic.AtomicInteger} counters and minimal
 * synchronized sections for window resets.
 * </p>
 *
 * Responses exceeding limits are short-circuited with HTTP 429 and a <code>Retry-After</code> header.
 */
@Component
@RequiredArgsConstructor
public class GlobalRateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;
    private final DynamicRouteService routeService;

    private AtomicInteger counter = new AtomicInteger(0);
    private volatile long windowStart = System.currentTimeMillis();


    private final AtomicInteger camelGlobalCounter = new AtomicInteger(0);
    private volatile long camelGlobalWindowStart = System.currentTimeMillis();

    // per-route counters
    private final Map<String, AtomicInteger> routeCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> routeWindowStarts = new ConcurrentHashMap<>();


    /**
     * Apply rate limiting to the incoming request.
     *
     * @param request  the incoming {@link ServletRequest}
     * @param response the outgoing {@link ServletResponse}
     * @param chain    the {@link FilterChain} to continue processing when allowed
     * @throws IOException      if I/O error occurs
     * @throws ServletException if the filter chain cannot be continued
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        if (uri.startsWith("/camel/")) {
            // === 1. Apply global Camel rate limit ===
            CamelLimitConfig globalConfig = rateLimitService.getCamelConfig();
            long now = System.currentTimeMillis();


            AntPathMatcher matcher = new AntPathMatcher();
            String normalizedPath = uri.replaceFirst("^/camel", "");


            synchronized (this) {
                if (now - camelGlobalWindowStart >= globalConfig.getSeconds() * 1000L) {
                    camelGlobalCounter.set(0);
                    camelGlobalWindowStart = now;
                }
            }

            if (camelGlobalCounter.incrementAndGet() > globalConfig.getRequestLimit()) {
                HttpServletResponse res = (HttpServletResponse) response;
                res.setStatus(429);
                res.setHeader("Retry-After", String.valueOf(globalConfig.getSeconds()));
                res.getWriter().write("Too Many Requests (Global Camel Limit). Try again later.");
                return;
            }

            // === 2. Apply route-specific limit if exists ===

            List<DynamicRouteEntity> dbRoutes = routeService.getActiveRoutes();


            String matchedRouteId = null;
            for (DynamicRouteEntity dbRoute : dbRoutes) {

                String routePath = dbRoute.getPath();
                String routeMethod = dbRoute.getHttpMethod();
                String pattern = routePath.replaceAll("\\{[^/]+\\}", "*") + "/**";

                if (matcher.match(pattern, normalizedPath)&&
                        method.equalsIgnoreCase(routeMethod)) {
                    matchedRouteId = routeService.getRouteIdByPathAndMethod(routePath,method);
                    break;
                }
            }
            Optional<CustomCamelLimitConfig> customConfigOpt = rateLimitService.findByRouteId(matchedRouteId);


            if (customConfigOpt.isPresent()) {
                CustomCamelLimitConfig config = customConfigOpt.get();
                String routeId = config.getRouteId();

                // init counter if needed
                routeCounters.putIfAbsent(routeId, new AtomicInteger(0));
                routeWindowStarts.putIfAbsent(routeId, now);

                synchronized (routeId.intern()) {
                    long routeWindowStart = routeWindowStarts.get(routeId);
                    if (now - routeWindowStart >= config.getSeconds() * 1000L) {
                        routeCounters.get(routeId).set(0);
                        routeWindowStarts.put(routeId, now);
                    }
                }

                if (routeCounters.get(routeId).incrementAndGet() > config.getRequestLimit()) {
                    HttpServletResponse res = (HttpServletResponse) response;
                    res.setStatus(429);
                    res.setHeader("Retry-After", String.valueOf(config.getSeconds()));
                    res.getWriter().write("Too Many Requests (Route Limit). Try again later.");
                    return;
                }
            }

            // pass along if both limits ok
            chain.doFilter(request, response);
            return;
        }


        RateLimitConfig config = rateLimitService.getConfig();
        long now = System.currentTimeMillis();

        synchronized (this) {
            // reset if window expired
            if (now - windowStart >= config.getWindowSeconds() * 1000L) {
                counter.set(0);
                windowStart = now;
            }
        }

        if (counter.incrementAndGet() > config.getLimitRequests()) {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(config.getWindowSeconds()));
            res.getWriter().write("Too Many Requests. Try again later.");
            return;
        }
        chain.doFilter(request, response);
    }
}