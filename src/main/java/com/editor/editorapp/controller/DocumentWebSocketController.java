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

        Operation transformedOp = otService.transform(id, incomingOp);

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