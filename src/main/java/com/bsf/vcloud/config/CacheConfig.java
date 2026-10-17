package com.bsf.vcloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
public class CacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .disableCachingNullValues();

        RedisCacheConfiguration userVideosConfig =  defaultConfig.entryTtl(Duration.ofMinutes(10));

        RedisCacheConfiguration thumbnailUrlsConfig = defaultConfig.entryTtl(Duration.ofMinutes(25));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("userVideos",userVideosConfig)
                .withCacheConfiguration("thumbnailUrls",thumbnailUrlsConfig)
                .build();
    }
}
