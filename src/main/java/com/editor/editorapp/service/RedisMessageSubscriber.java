package com.editor.editorapp.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessageSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    public RedisMessageSubscriber(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Called when a message is received from Redis pub/sub.
     * Forwards the message to the appropriate WebSocket topic.
     *
     * Redis channel format: doc:{documentId}:ops  or  doc:{documentId}:cursors
     */
    public void onMessage(String message, String channel) {
        System.out.println("📨 Redis received on " + channel + ": " + message);

        try {
            // Parse channel: doc:123:ops → documentId=123, type=ops
            String[] parts = channel.split(":");
            if (parts.length >= 3) {
                String documentId = parts[1];
                String type = parts[2];

                String wsTopic;
                if ("ops".equals(type)) {
                    wsTopic = "/topic/document/" + documentId;
                } else if ("cursors".equals(type)) {
                    wsTopic = "/topic/document/" + documentId + "/cursors";
                } else {
                    return;
                }

                // Forward to WebSocket clients connected to THIS server
                messagingTemplate.convertAndSend(wsTopic, message);
            }
        } catch (Exception e) {
            System.err.println("Failed to process Redis message: " + e.getMessage());
        }
    }
}