package com.editor.editorapp.service;

import com.editor.editorapp.model.Document;
import com.editor.editorapp.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Document createDocument(String title, String content) {
        Document doc = new Document(title, content);
        return documentRepository.save(doc);
    }

    public Optional<Document> getDocument(Long id) {
        return documentRepository.findById(id);
    }

    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Document updateDocument(Long id, String newContent) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        doc.setContent(newContent);
        return documentRepository.save(doc);
    }

    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }
}