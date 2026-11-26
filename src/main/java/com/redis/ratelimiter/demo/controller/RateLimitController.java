package com.redis.ratelimiter.demo.controller;

import com.redis.ratelimiter.demo.limiter.RateLimitDecision;
import com.redis.ratelimiter.demo.limiter.RateLimiterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class RateLimitController {

    private final RateLimiterService rateLimiterService;

    public RateLimitController(RateLimiterService rateLimiterService
    ) {
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestHeader("X-Demo-Customer-Id") String customerId
    ) {

        return handleRequest(
                customerId,
                "GET:/api/orders"
        );
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestHeader("X-Demo-Customer-Id") String customerId
    ) {

        return handleRequest(
                customerId,
                "POST:/api/orders"
        );
    }

    private ResponseEntity<Map<String, Object>> handleRequest(
            String customerId,
            String endpointId
    ) {

        RateLimitDecision decision = rateLimiterService.evaluate(
                        customerId,
                        endpointId
                );

        if (!decision.allowed()) {

            long retryAfterSeconds = Math.max(
                    1,
                    (long) Math.ceil(
                            decision.retryAfterMillis() / 1000.0
                    )
            );

            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(
                            "Retry-After",
                            String.valueOf(retryAfterSeconds)
                    )
                    .header(
                            "X-RateLimit-Global-Remaining",
                            String.valueOf(
                                    Math.floor(decision.globalRemaining())
                            )
                    )
                    .header(
                            "X-RateLimit-Remaining",
                            String.valueOf(
                                    Math.floor(decision.endpointRemaining())
                            )
                    )
                    .body(Map.of(
                            "status", 429,
                            "message", "Rate limit exceeded",
                            "retryAfterSeconds", retryAfterSeconds
                    ));
        }

        return ResponseEntity.ok()
                .header(
                        "X-RateLimit-Global-Remaining",
                        String.valueOf(
                                Math.floor(decision.globalRemaining())
                        )
                )
                .header(
                        "X-RateLimit-Remaining",
                        String.valueOf(
                                Math.floor(decision.endpointRemaining())
                        )
                )
                .body(Map.of(
                        "status", 200,
                        "message", "Request allowed",
                        "globalRemaining", decision.globalRemaining(),
                        "endpointRemaining", decision.endpointRemaining()
                ));
    }
}