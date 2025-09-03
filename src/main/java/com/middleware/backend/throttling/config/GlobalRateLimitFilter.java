package com.middleware.backend.throttling.config;
import com.middleware.backend.throttling.model.RateLimitConfig;
import com.middleware.backend.throttling.service.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class GlobalRateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;

    private AtomicInteger counter = new AtomicInteger(0);
    private volatile long windowStart = System.currentTimeMillis();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

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