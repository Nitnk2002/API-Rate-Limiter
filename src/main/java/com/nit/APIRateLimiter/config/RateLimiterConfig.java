package com.nit.APIRateLimiter.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.TimeoutOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RedisClient redisClient() {
        RedisClient client = RedisClient.create("redis://localhost:6379");

        // Configure Lettuce to fail quickly when commands don't finish
        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
                .fixedTimeout(Duration.ofMillis(100)) // Force commands to abort after 100ms
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .timeoutOptions(timeoutOptions)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS) // Fail fast if disconnected
                .build();

        client.setOptions(clientOptions);
        return client;
    }

    @Bean
    public ProxyManager<byte[]> proxyManager(RedisClient redisClient) {

        // 1. Define the Client Configuration (The proper Bucket4j 8.x approach)
        ClientSideConfig clientConfig = ClientSideConfig.getDefault()
                .withExpirationAfterWriteStrategy(
                        // This tells Redis to automatically delete the key 10 seconds
                        // after the bucket has fully refilled to save memory.
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10))
                );

        // 2. Build and return the ProxyManager
        return LettuceBasedProxyManager.builderFor(redisClient)
                .withClientSideConfig(clientConfig)
                .build();
    }
}
