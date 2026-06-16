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
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;

@Configuration
public class RateLimiterConfig {

    @Value("${REDIS_URL}")
    private String redisUrl;

    @Bean
    public RedisClient redisClient() {

        RedisClient client = RedisClient.create(redisUrl);

        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
                .fixedTimeout(Duration.ofMillis(100))
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .timeoutOptions(timeoutOptions)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();

        client.setOptions(clientOptions);
        return client;
    }

    @Bean
    public ProxyManager<byte[]> proxyManager(RedisClient redisClient) {

        ClientSideConfig clientConfig = ClientSideConfig.getDefault()
                .withExpirationAfterWriteStrategy(
                        ExpirationAfterWriteStrategy
                                .basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10))
                );

        return LettuceBasedProxyManager.builderFor(redisClient)
                .withClientSideConfig(clientConfig)
                .build();
    }
}
