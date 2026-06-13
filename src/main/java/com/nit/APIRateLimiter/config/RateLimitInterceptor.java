package com.nit.APIRateLimiter.config;

import com.nit.APIRateLimiter.service.RateLimiterService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String apiKey = request.getHeader("X-API-KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("{\"error\": \"Missing X-API-KEY header\"}");
            return false;
        } // Or extract an API key from request.getHeader("X-API-KEY")

        try {
            Bucket bucket = rateLimiterService.resolveBucket(apiKey);
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                // Production Best Practice: Always send standard limit tracking headers
                response.addHeader("X-Rate-Limit-Limit", "5");
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                return true;
            } else {
                // Calculate exactly when tokens will reset (in seconds)
                long waitForRefillSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefillSeconds));

                // Send a clean JSON error structure instead of a plain text string
                response.getWriter().write(String.format(
                        "{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again in %d seconds.\"}",
                        waitForRefillSeconds
                ));
                return false;
            }
        } catch (Exception e) {
            // Step 2 below implements the 'Fail-Open' logic here
            System.err.println("Rate Limiter Error (Redis down?): " + e.getMessage());
            return true; // ALLOW the request anyway so the core app doesn't break!
        }
    }
}