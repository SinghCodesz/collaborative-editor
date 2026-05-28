package com.editor.editorapp.service;

import com.editor.editorapp.model.Operation;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OTService {

    // Store recent operations per document for conflict detection
    // docId → list of operations received in the last 500ms
    private final Map<Long, List<Operation>> recentOps = new ConcurrentHashMap<>();

    /**
     * Transform an incoming operation against concurrent operations.
     * Returns the transformed operation ready for broadcast.
     */
    public Operation transform(Long documentId, Operation incomingOp) {

        // Get concurrent operations (received in last 500ms from other users)
        List<Operation> concurrentOps = getConcurrentOps(documentId, incomingOp);

        Operation transformed = incomingOp;

        // Transform against each concurrent operation
        for (Operation concurrent : concurrentOps) {
            transformed = transformPair(transformed, concurrent);
        }

        // Store this operation for future transformations
        storeOperation(documentId, incomingOp);

        return transformed;
    }

    /**
     * Transform op1 against op2.
     * op1 is the operation being transformed.
     * op2 is the concurrent operation.
     */
    private Operation transformPair(Operation op1, Operation op2) {

        // Same user? No conflict (shouldn't happen, but safety check)
        if (op1.getUserId().equals(op2.getUserId())) {
            return op1;
        }

        // Both INSERTS
        if (op1.getType() == Operation.OpType.INSERT &&
                op2.getType() == Operation.OpType.INSERT) {
            return transformInsertInsert(op1, op2);
        }

        // Both DELETES
        if (op1.getType() == Operation.OpType.DELETE &&
                op2.getType() == Operation.OpType.DELETE) {
            return transformDeleteDelete(op1, op2);
        }

        // INSERT vs DELETE
        if (op1.getType() == Operation.OpType.INSERT &&
                op2.getType() == Operation.OpType.DELETE) {
            return transformInsertDelete(op1, op2);
        }

        // DELETE vs INSERT
        if (op1.getType() == Operation.OpType.DELETE &&
                op2.getType() == Operation.OpType.INSERT) {
            return transformDeleteInsert(op1, op2);
        }

        return op1;
    }

    /**
     * Two inserts at the same position.
     * Rule: Lower userId keeps position, higher userId shifts right.
     * If different positions: the one at higher position shifts right
     * if other insert is before it.
     */
    private Operation transformInsertInsert(Operation op1, Operation op2) {
        if (op1.getPosition() < op2.getPosition()) {
            // op1 is before op2 → no change to op1
            return op1;
        } else if (op1.getPosition() > op2.getPosition()) {
            // op2 is before op1 → shift op1 right
            op1.setPosition(op1.getPosition() + 1);
            return op1;
        } else {
            // Same position → use userId to decide order
            if (op1.getUserId().compareTo(op2.getUserId()) < 0) {
                // op1 has lower userId → stays at position
                return op1;
            } else {
                // op1 has higher userId → shifts right
                op1.setPosition(op1.getPosition() + 1);
                return op1;
            }
        }
    }

    /**
     * Both deletes at the same position.
     * The second delete has nothing to delete.
     */
    private Operation transformDeleteDelete(Operation op1, Operation op2) {
        if (op1.getPosition() == op2.getPosition()) {
            // Both deleting same character → second delete ignored
            op1.setType(Operation.OpType.DELETE);
            op1.setCharacter("");  // Empty delete = no-op
            return op1;
        } else if (op1.getPosition() > op2.getPosition()) {
            // op2 deleted a character before op1's position → shift left
            op1.setPosition(op1.getPosition() - 1);
            return op1;
        }
        // op1.position < op2.position → no change
        return op1;
    }

    /**
     * Insert vs Delete.
     * op1 = INSERT, op2 = DELETE
     */
    private Operation transformInsertDelete(Operation insert, Operation delete) {
        if (insert.getPosition() <= delete.getPosition()) {
            // Insert is at or before delete position → no change
            return insert;
        } else {
            // Insert is after delete position → shift left
            insert.setPosition(insert.getPosition() - 1);
            return insert;
        }
    }

    /**
     * Delete vs Insert.
     * op1 = DELETE, op2 = INSERT
     */
    private Operation transformDeleteInsert(Operation delete, Operation insert) {
        if (delete.getPosition() < insert.getPosition()) {
            // Delete is before insert → no change
            return delete;
        } else {
            // Delete is at or after insert → shift right
            delete.setPosition(delete.getPosition() + 1);
            return delete;
        }
    }

    /**
     * Get concurrent operations from other users in the last 500ms.
     */
    private List<Operation> getConcurrentOps(Long documentId, Operation op) {
        List<Operation> docOps = recentOps.getOrDefault(documentId, new ArrayList<>());
        List<Operation> concurrent = new ArrayList<>();

        // Create a snapshot to avoid ConcurrentModificationException
        List<Operation> snapshot;
        synchronized (docOps) {
            snapshot = new ArrayList<>(docOps);
        }

        long now = System.currentTimeMillis();
        long window = 500;

        for (Operation existing : snapshot) {
            if (!existing.getUserId().equals(op.getUserId()) &&
                    (now - existing.getTimestamp()) < window) {
                concurrent.add(existing);
            }
        }

        return concurrent;
    }

    /**
     * Store an operation in the recent operations buffer.
     */
    private void storeOperation(Long documentId, Operation op) {
        List<Operation> docOps = recentOps.computeIfAbsent(documentId, k -> new ArrayList<>());

        synchronized (docOps) {
            docOps.add(op);

            // Cleanup old operations (older than 2 seconds)
            docOps.removeIf(
                    existing -> System.currentTimeMillis() - existing.getTimestamp() > 2000
            );
        }
    }

    /**
     * Apply an operation to a document's text content.
     */
    public String applyOperation(String content, Operation op) {
        if (op.getType() == Operation.OpType.INSERT) {
            // Insert character at position
            int pos = Math.min(op.getPosition(), content.length());
            return content.substring(0, pos) +
                    op.getCharacter() +
                    content.substring(pos);
        } else if (op.getType() == Operation.OpType.DELETE) {
            // Delete character at position
            if (op.getPosition() >= 0 && op.getPosition() < content.length()) {
                return content.substring(0, op.getPosition()) +
                        content.substring(op.getPosition() + 1);
            }
        }
        return content;
    }
}