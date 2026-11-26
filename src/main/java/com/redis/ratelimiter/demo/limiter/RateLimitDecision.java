package com.redis.ratelimiter.demo.limiter;

public record RateLimitDecision(
        boolean allowed,
        double globalRemaining,
        double endpointRemaining,
        long retryAfterMillis
) {}
