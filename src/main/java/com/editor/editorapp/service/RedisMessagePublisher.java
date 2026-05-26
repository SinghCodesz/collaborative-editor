package com.editor.editorapp.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessagePublisher {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisMessagePublisher(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Publish an operation to Redis for cross-server broadcasting.
     */
    public void publishOperation(Long documentId, String message) {
        String channel = "doc:" + documentId + ":ops";
        redisTemplate.convertAndSend(channel, message);
        System.out.println("📤 Redis published to " + channel);
    }

    /**
     * Publish cursor updates to Redis for cross-server broadcasting.
     */
    public void publishCursor(Long documentId, String message) {
        String channel = "doc:" + documentId + ":cursors";
        redisTemplate.convertAndSend(channel, message);
    }
}