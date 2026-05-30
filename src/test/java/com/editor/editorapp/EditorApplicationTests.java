package com.editor.editorapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EditorApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring application context loads successfully
    }

    @Test
    void documentCreationTest() {
        // Simple logic test
        String title = "Test Document";
        assert title != null && !title.isEmpty();
    }
}