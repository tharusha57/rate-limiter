package com.redis.ratelimiter.demo.policy;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RateLimitPolicyService {

    private final BucketPolicy globalPolicy = new BucketPolicy(200, 20);

    private final Map<String, BucketPolicy> endpointPolicies =
            Map.of(
                    "GET:/api/orders",
                    new BucketPolicy(100, 10),

                    "POST:/api/orders",
                    new BucketPolicy(20, 2)
            );

    public BucketPolicy getGlobalPolicy(String customerId) {

        // TODO:
        // Retrieve customer-specific global policy.

        return globalPolicy;
    }

    public BucketPolicy getEndpointPolicy(
            String customerId,
            String endpointId
    ) {

        BucketPolicy policy = endpointPolicies.get(endpointId);

        if (policy == null) {
            throw new IllegalArgumentException(
                    "No rate limit policy configured for: " + endpointId
            );
        }

        return policy;
    }
}