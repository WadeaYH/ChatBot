package com.yu.chatbot.service;

import com.yu.chatbot.model.WebDocument;
import com.yu.chatbot.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;

    public WebDocument save(WebDocument document) {
        log.info("Saving document: {}", document.getUrl());
        return documentRepository.save(document);
    }

    public List<WebDocument> findAll() {
        return documentRepository.findAll();
    }

    public Optional<WebDocument> findById(String id) {
        return documentRepository.findById(id);
    }

    public Optional<WebDocument> findByUrl(String url) {
        return documentRepository.findByUrl(url);
    }

    public List<WebDocument> searchByKeyword(String keyword) {
        log.info("Searching for keyword: {}", keyword);
        return documentRepository.searchByKeyword(keyword);
    }

    public List<WebDocument> findByContentContaining(String keyword) {
        return documentRepository.findByContentContaining(keyword);
    }

    public List<WebDocument> findByFileType(String fileType) {
        return documentRepository.findByFileType(fileType);
    }

    public WebDocument update(String id, WebDocument updatedDocument) {
        return documentRepository.findById(id)
                .map(existing -> {
                    existing.setTitle(updatedDocument.getTitle());
                    existing.setContent(updatedDocument.getContent());
                    existing.setFileType(updatedDocument.getFileType());
                    return documentRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
    }

    public void deleteById(String id) {
        log.info("Deleting document: {}", id);
        documentRepository.deleteById(id);
    }

    public void deleteAll() {
        log.warn("Deleting ALL documents");
        documentRepository.deleteAll();
    }

    public long count() {
        return documentRepository.count();
    }

    public long countByFileType(String fileType) {
        return documentRepository.countByFileType(fileType);
    }
}