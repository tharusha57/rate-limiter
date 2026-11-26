package com.redis.ratelimiter.demo.limiter;

import com.redis.ratelimiter.demo.policy.BucketPolicy;
import com.redis.ratelimiter.demo.policy.RateLimitPolicyService;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final RateLimitPolicyService policyService;

    private final RedisRateLimiterRepository redisRepository;

    public RateLimiterService(
            RateLimitPolicyService policyService,
            RedisRateLimiterRepository redisRepository
    ) {
        this.policyService = policyService;
        this.redisRepository = redisRepository;
    }

    public RateLimitDecision evaluate(
            String customerId,
            String endpointId
    ) {

        BucketPolicy globalPolicy =
                policyService.getGlobalPolicy(customerId);

        BucketPolicy endpointPolicy =
                policyService.getEndpointPolicy(
                        customerId,
                        endpointId
                );

        return redisRepository.evaluate(
                customerId,
                endpointId,
                globalPolicy,
                endpointPolicy
        );
    }
}