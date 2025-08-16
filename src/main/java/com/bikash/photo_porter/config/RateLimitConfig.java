package com.bikash.photo_porter.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public Bucket rateLimitBucket() {
        // allow 100 reqs per user per hour
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofHours(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }
} 