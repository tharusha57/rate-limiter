package com.redis.ratelimiter.demo.limiter;

import com.redis.ratelimiter.demo.policy.BucketPolicy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RedisRateLimiterRepository {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> tokenBucketScript;

    public RedisRateLimiterRepository(
            StringRedisTemplate redisTemplate,
            RedisScript<List> tokenBucketScript
    ) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = tokenBucketScript;
    }

    public RateLimitDecision evaluate(String customerId, String endpointId,
                                      BucketPolicy globalPolicy, BucketPolicy endpointPolicy
    ) {

        String globalKey =
                "rate_limit:{" + customerId + "}:global";

        String endpointKey =
                "rate_limit:{" + customerId
                        + "}:endpoint:" + endpointId;

        List<?> result = redisTemplate.execute(
                tokenBucketScript,
                List.of(globalKey, endpointKey),

                String.valueOf(globalPolicy.capacity()),
                String.valueOf(globalPolicy.refillRatePerSecond()),

                String.valueOf(endpointPolicy.capacity()),
                String.valueOf(endpointPolicy.refillRatePerSecond())
        );

        if (result == null || result.size() != 4) {
            throw new IllegalStateException(
                    "Invalid response from Redis rate limiter"
            );
        }

        boolean allowed =
                Long.parseLong(result.get(0).toString()) == 1;

        double globalRemaining =
                Double.parseDouble(result.get(1).toString());

        double endpointRemaining =
                Double.parseDouble(result.get(2).toString());

        long retryAfterMillis =
                Long.parseLong(result.get(3).toString());

        return new RateLimitDecision(
                allowed,
                globalRemaining,
                endpointRemaining,
                retryAfterMillis
        );
    }
}
