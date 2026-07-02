package com.gentlemanstore.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AuthRateLimitingFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 5;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
    // Upper bound on tracked client buckets; prevents unbounded memory growth if an
    // attacker rotates source IPs. Once full, all buckets are refilled within a minute
    // anyway, so resetting the map is safe.
    private static final int MAX_TRACKED_BUCKETS = 10_000;

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isRateLimited = path.endsWith("/api/auth/login") || path.endsWith("/api/auth/register");

        if (isRateLimited) {
            String key = request.getRemoteAddr() + ":" + path;
            if (buckets.size() >= MAX_TRACKED_BUCKETS && !buckets.containsKey(key)) {
                buckets.clear();
            }
            Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder()
                    .addLimit(Bandwidth.classic(CAPACITY, Refill.greedy(CAPACITY, REFILL_PERIOD)))
                    .build());

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
