package com.editor.editorapp.repository;

import com.editor.editorapp.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    // Spring Data JPA provides:
    // save(), findById(), findAll(), deleteById() automatically
}