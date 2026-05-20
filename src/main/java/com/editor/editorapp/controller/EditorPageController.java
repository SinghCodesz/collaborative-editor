package com.editor.editorapp.controller;

import com.editor.editorapp.model.Document;
import com.editor.editorapp.service.DocumentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller  // NOT @RestController — this returns HTML, not JSON
public class EditorPageController {

    private final DocumentService documentService;

    public EditorPageController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/editor/{id}")
    public String editorPage(@PathVariable Long id, Model model) {
        Document doc = documentService.getDocument(id)
                .orElse(null);
        if (doc == null) {
            doc = documentService.createDocument("Untitled", "");
        }
        model.addAttribute("document", doc);
        return "editor";
    }
}