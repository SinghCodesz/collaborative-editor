package com.editor.editorapp.controller;

import com.editor.editorapp.model.Operation;
import com.editor.editorapp.service.DocumentService;
import com.editor.editorapp.service.OTService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class DocumentWebSocketController {

    private final OTService otService;
    private final DocumentService documentService;

    public DocumentWebSocketController(OTService otService, DocumentService documentService) {
        this.otService = otService;
        this.documentService = documentService;
    }

    @MessageMapping("/document/{id}/edit")
    @SendTo("/topic/document/{id}")
    public Map<String, Object> handleEdit(
            @DestinationVariable Long id,
            Map<String, Object> payload) {

        // Extract operation from payload
        String type = (String) payload.get("type");
        int position = (int) payload.get("position");
        String character = (String) payload.getOrDefault("character", "");
        String userId = (String) payload.get("userId");
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

        // Transform the operation against concurrent operations
        Operation transformedOp = otService.transform(id, incomingOp);

        System.out.println("🔄 Transformed: " + transformedOp);

        // Apply to the authoritative document state
        documentService.getDocument(id).ifPresent(doc -> {
            String newContent = otService.applyOperation(doc.getContent(), transformedOp);
            doc.setContent(newContent);
            documentService.updateDocument(id, newContent);
        });

        // Return transformed operation to broadcast
        return Map.of(
                "type", transformedOp.getType().name(),
                "position", transformedOp.getPosition(),
                "character", transformedOp.getCharacter(),
                "userId", transformedOp.getUserId(),
                "timestamp", transformedOp.getTimestamp(),
                "sequenceNumber", transformedOp.getSequenceNumber()
        );
    }
}