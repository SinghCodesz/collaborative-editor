package com.editor.editorapp.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class DocumentWebSocketController {

    /**
     * Client sends to: /app/document/{id}/edit
     * Server broadcasts to: /topic/document/{id}
     */
    @MessageMapping("/document/{id}/edit")
    @SendTo("/topic/document/{id}")
    public Map<String, Object> handleEdit(
            @DestinationVariable Long id,
            Map<String, Object> payload) {

        System.out.println("📝 Received edit for doc " + id);
        System.out.println("   Content length: " +
                payload.get("content").toString().length());
        System.out.println("   User: " + payload.get("userId"));

        // For Day 2: Echo back (no conflict resolution yet)
        // Day 3: We'll add OT transformation here
        return payload;
    }
}