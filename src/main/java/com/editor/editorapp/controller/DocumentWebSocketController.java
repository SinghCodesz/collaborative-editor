package com.editor.editorapp.controller;

import com.editor.editorapp.model.Operation;
import com.editor.editorapp.service.DocumentService;
import com.editor.editorapp.service.OTService;
import com.editor.editorapp.util.RateLimiter;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class DocumentWebSocketController {

    private final OTService otService;
    private final DocumentService documentService;
    private final RateLimiter ratelimiter;

    public DocumentWebSocketController(OTService otService, DocumentService documentService) {
        this.otService = otService;
        this.documentService = documentService;
        this.ratelimiter = new RateLimiter(10); // 10 ops/sec per user
    }

    @MessageMapping("/document/{id}/edit")
    @SendTo("/topic/document/{id}")
    public Map<String, Object> handleEdit(
            @DestinationVariable Long id,
            Map<String, Object> payload) {


        String userId = (String) payload.get("userId");

        //RATE LIMIT CHECK
        if (!ratelimiter.allowRequest(userId)) {
            System.out.println("⚠️ Rate limited user: " + userId);
            return Map.of("type", "RATE_LIMITED", "userId", userId);
        }

        String type = (String) payload.get("type");
        int position = (int) payload.get("position");
        String character = (String) payload.getOrDefault("character", "");
        long timestamp = (long) payload.getOrDefault("timestamp", System.currentTimeMillis());
        int sequenceNumber = (int) payload.getOrDefault("sequenceNumber", 0);

        Operation incomingOp = new Operation(
                Operation.OpType.valueOf(type),
                position,
                character,
                userId,
                timestamp,
                sequenceNumber
        );

        System.out.println("📝 Received: " + incomingOp);

        Operation transformedOp = otService.transform(id, incomingOp);

        System.out.println("🔄 Transformed: " + transformedOp);

        documentService.getDocument(id).ifPresent(doc -> {
            String newContent = otService.applyOperation(doc.getContent(), transformedOp);
            doc.setContent(newContent);
            documentService.updateDocument(id, newContent);
        });

        return Map.of(
                "type", transformedOp.getType().name(),
                "position", transformedOp.getPosition(),
                "character", transformedOp.getCharacter(),
                "userId", transformedOp.getUserId(),
                "timestamp", transformedOp.getTimestamp(),
                "sequenceNumber", transformedOp.getSequenceNumber()
        );
    }

    @MessageMapping("/document/{id}/cursor")
    @SendTo("/topic/document/{id}/cursors")
    public Map<String, Object> handleCursor(
            @DestinationVariable Long id,
            Map<String, Object> payload) {

        System.out.println("Cursor update for doc " + id +
                " from " + payload.get("userName") +
                " at position " + payload.get("position"));

        return payload;
    }
}