package com.nit.APIRateLimiter.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

    private final ProxyManager<byte[]> proxyManager;

    public RateLimiterService(ProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    public Bucket resolveBucket(String key) {
        byte[] keyBytes = key.getBytes();

        // Simulating tier checking. In a real app, you'd check a DB or JWT claim.
        String tier = determineTier(key);

        return proxyManager.builder().build(keyBytes, () -> getConfigForTier(tier));
    }

    private String determineTier(String apiKey) {
        // If they pass a key starting with "premium_", give them 100 requests
        if (apiKey.startsWith("premium_")) {
            return "PREMIUM";
        }
        // Otherwise, everyone else gets the default 5 requests
        return "FREE";
    }

    private BucketConfiguration getConfigForTier(String tier) {
        if ("PREMIUM".equals(tier)) {
            // Premium: 100 requests per minute
            return BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                    .build();
        }

        // Default / Free Tier: 5 requests per minute
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                .build();
    }
}
