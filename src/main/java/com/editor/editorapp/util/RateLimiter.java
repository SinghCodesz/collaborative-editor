package com.editor.editorapp.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Simple token bucket rate limiter.
 * Each user gets a bucket that refills at a fixed rate.
 */
public class RateLimiter {

    private final int maxRequestsPerSecond;
    private final ConcurrentHashMap<String, UserBucket> userBuckets;

    public RateLimiter(int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
        this.userBuckets = new ConcurrentHashMap<>();
    }

    /**
     * Check if a request is allowed for this user.
     * Returns true if allowed, false if rate limited.
     */
    public boolean allowRequest(String userId) {
        UserBucket bucket = userBuckets.computeIfAbsent(userId,
                k -> new UserBucket(maxRequestsPerSecond));

        return bucket.tryConsume();
    }

    /**
     * Get remaining tokens for a user.
     */
    public int getRemainingTokens(String userId) {
        UserBucket bucket = userBuckets.get(userId);
        return bucket != null ? bucket.getTokens() : maxRequestsPerSecond;
    }

    /**
     * Clean up inactive users (call periodically).
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        userBuckets.entrySet().removeIf(entry ->
                now - entry.getValue().getLastAccess() > TimeUnit.MINUTES.toMillis(5)
        );
    }

    /**
     * Token bucket per user.
     */
    private static class UserBucket {
        private final int maxTokens;
        private double tokens;
        private long lastRefillTimestamp;
        private long lastAccess;

        UserBucket(int maxTokens) {
            this.maxTokens = maxTokens;
            this.tokens = maxTokens;
            this.lastRefillTimestamp = System.currentTimeMillis();
            this.lastAccess = System.currentTimeMillis();
        }

        /**
         * Try to consume one token. Returns true if successful.
         */
        synchronized boolean tryConsume() {
            refill();
            lastAccess = System.currentTimeMillis();

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        /**
         * Refill tokens based on elapsed time.
         */
        private void refill() {
            long now = System.currentTimeMillis();
            double elapsedSeconds = (now - lastRefillTimestamp) / 1000.0;

            // Refill: tokens per second × elapsed time
            double refillAmount = elapsedSeconds * maxTokens;
            tokens = Math.min(maxTokens, tokens + refillAmount);
            lastRefillTimestamp = now;
        }

        int getTokens() {
            refill();
            return (int) tokens;
        }

        long getLastAccess() {
            return lastAccess;
        }
    }
}