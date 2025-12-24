package com.yu.chatbot.controller;

import com.yu.chatbot.model.WebDocument;
import com.yu.chatbot.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {
    
    private final DocumentService documentService;
    
    // ==================== GET ====================
    
    /**
     * GET /api/documents - Get all documents
     */
    @GetMapping
    public ResponseEntity<List<WebDocument>> getAllDocuments() {
        List<WebDocument> documents = documentService.findAll();
        return ResponseEntity.ok(documents);
    }
    
    /**
     * GET /api/documents/{id} - Get by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<WebDocument> getDocumentById(@PathVariable String id) {
        return documentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/documents/search?q=keyword - Search with citation
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchDocuments(@RequestParam("q") String keyword) {
        List<WebDocument> results = documentService.searchByKeyword(keyword);
        
        Map<String, Object> response = new HashMap<>();
        response.put("keyword", keyword);
        response.put("count", results.size());
        response.put("results", results);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/documents/url?url=... - Get by URL (citation)
     */
    @GetMapping("/url")
    public ResponseEntity<WebDocument> getDocumentByUrl(@RequestParam String url) {
        return documentService.findByUrl(url)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/documents/type/{fileType} - Get by file type
     */
    @GetMapping("/type/{fileType}")
    public ResponseEntity<List<WebDocument>> getDocumentsByType(@PathVariable String fileType) {
        List<WebDocument> documents = documentService.findByFileType(fileType.toUpperCase());
        return ResponseEntity.ok(documents);
    }
    
    /**
     * GET /api/documents/stats - Get statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", documentService.count());
        stats.put("htmlPages", documentService.countByFileType("HTML"));
        stats.put("pdfFiles", documentService.countByFileType("PDF"));
        stats.put("wordFiles", documentService.countByFileType("DOCX"));
        return ResponseEntity.ok(stats);
    }
    
    // ==================== POST ====================
    
    /**
     * POST /api/documents - Create new document
     */
    @PostMapping
    public ResponseEntity<WebDocument> createDocument(@RequestBody WebDocument document) {
        WebDocument saved = documentService.save(document);
        return ResponseEntity.ok(saved);
    }
    
    // ==================== PUT ====================
    
    /**
     * PUT /api/documents/{id} - Update document
     */
    @PutMapping("/{id}")
    public ResponseEntity<WebDocument> updateDocument(
            @PathVariable String id,
            @RequestBody WebDocument document) {
        try {
            WebDocument updated = documentService.update(id, document);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // ==================== DELETE ====================
    
    /**
     * DELETE /api/documents/{id} - Delete document
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String id) {
        documentService.deleteById(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("deleted", true);
        response.put("id", id);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * DELETE /api/documents/all - Delete all
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> deleteAllDocuments() {
        long count = documentService.count();
        documentService.deleteAll();
        
        Map<String, Object> response = new HashMap<>();
        response.put("deleted", true);
        response.put("count", count);
        
        return ResponseEntity.ok(response);
    }
}
