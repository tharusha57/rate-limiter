package com.redis.ratelimiter.demo.policy;

public record BucketPolicy(
        int capacity,
        double refillRatePerSecond
) {
    public BucketPolicy {
        if (capacity < 1 || refillRatePerSecond <= 0) {
            throw new IllegalArgumentException(
                    "Invalid bucket policy"
            );
        }
    }
}