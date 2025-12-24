package com.yu.chatbot.controller;

import com.yu.chatbot.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID; // Import UUID

@RestController
@RequestMapping("/api/crawler")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CrawlerController {

    private final CrawlerService crawlerService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startCrawling(@RequestBody CrawlRequest request) {

        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
        }

        int maxDepth = request.getMaxDepth() > 0 ? request.getMaxDepth() : 3;

        String jobId = UUID.randomUUID().toString();

        crawlerService.startCrawling(request.getUrl(), maxDepth, jobId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "started");
        response.put("jobId", jobId);
        response.put("url", request.getUrl());
        response.put("maxDepth", maxDepth);
        response.put("message", "Crawling started. Use /api/crawler/status/" + jobId + " to check progress");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<Object> getCrawlStatus(@PathVariable String jobId) {
        CrawlerService.CrawlStatus status = crawlerService.getCrawlStatus(jobId);

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(status);
    }

    @lombok.Data
    public static class CrawlRequest {
        private String url;
        private int maxDepth = 3;
    }
}