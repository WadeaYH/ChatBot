package com.yu.chatbot.service;

import com.yu.chatbot.model.WebDocument;
import com.yu.chatbot.repository.DocumentRepository;
import com.yu.chatbot.service.extractor.HtmlExtractor;
import com.yu.chatbot.service.extractor.PdfExtractor;
import com.yu.chatbot.service.extractor.WordExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlerService {

    private final DocumentRepository documentRepository;
    private final HtmlExtractor htmlExtractor;
    private final PdfExtractor pdfExtractor;
    private final WordExtractor wordExtractor;

    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    private final Map<String, CrawlStatus> crawlJobs = new ConcurrentHashMap<>();

    @Async
    public void startCrawling(String rootUrl, int maxDepth, String jobId) {

        System.out.println("Starting Crawler");
        CrawlStatus status = new CrawlStatus();
        status.setStatus("RUNNING");
        status.setStartTime(LocalDateTime.now());
        crawlJobs.put(jobId, status);

        visitedUrls.clear();

        String baseDomain = extractDomain(rootUrl);

        log.info("Starting crawl job {} for URL: {} with maxDepth: {}", jobId, rootUrl, maxDepth);

        try {
            crawlRecursive(rootUrl, baseDomain, 0, maxDepth, status);

            status.setStatus("COMPLETED");
            status.setEndTime(LocalDateTime.now());
            log.info("Crawl job {} completed. Total pages: {}", jobId, status.getTotalPages());

        } catch (Exception e) {
            status.setStatus("FAILED");
            status.setError(e.getMessage());
            log.error("Crawl job {} failed: {}", jobId, e.getMessage());
        }
    }

    private void crawlRecursive(String url, String baseDomain, int currentDepth,
                                int maxDepth, CrawlStatus status) {

        if (currentDepth > maxDepth) return;
        if (visitedUrls.contains(url)) return;

        if (documentRepository.existsByUrl(url)) {
            visitedUrls.add(url);
            return;
        }

        visitedUrls.add(url);
        status.incrementTotalPages();

        log.info("Crawling [depth={}]: {}", currentDepth, url);

        try {
            String fileType = determineFileType(url);
            String content = "";
            String title = "";

            switch (fileType) {
                case "HTML":
                    content = htmlExtractor.extractText(url);
                    title = htmlExtractor.extractTitle(url);

                    if (currentDepth < maxDepth) {
                        Set<String> links = htmlExtractor.extractLinks(url, baseDomain);
                        Set<String> fileLinks = htmlExtractor.extractFileLinks(url);

                        for (String link : links) {
                            crawlRecursive(link, baseDomain, currentDepth + 1, maxDepth, status);
                        }

                        for (String fileLink : fileLinks) {
                            crawlRecursive(fileLink, baseDomain, currentDepth + 1, maxDepth, status);
                        }
                    }
                    break;

                case "PDF":
                    content = pdfExtractor.extractText(url);
                    title = extractFilename(url);
                    break;

                case "DOCX":
                case "DOC":
                    content = wordExtractor.extractText(url);
                    title = extractFilename(url);
                    break;

                default:
                    return;
            }

            if (content == null || content.trim().isEmpty()) {
                return;
            }

            WebDocument document = WebDocument.builder()
                    .url(url)
                    .title(title)
                    .content(content)
                    .fileType(fileType)
                    .crawledAt(LocalDateTime.now())
                    .contentHash(generateHash(content))
                    .status("SUCCESS")
                    .depth(currentDepth)
                    .build();

            documentRepository.save(document);
            status.incrementSuccessPages();

            log.info("Saved document: {} ({} chars)", url, content.length());

        } catch (Exception e) {
            status.incrementFailedPages();
            log.error("Failed to crawl {}: {}", url, e.getMessage());
        }
    }

    private String determineFileType(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".pdf")) return "PDF";
        if (lowerUrl.endsWith(".docx")) return "DOCX";
        if (lowerUrl.endsWith(".doc")) return "DOC";
        if (lowerUrl.endsWith(".xlsx") || lowerUrl.endsWith(".xls")) return "EXCEL";
        if (lowerUrl.endsWith(".txt")) return "TEXT";
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".png") ||
                lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".gif")) return "IMAGE";
        return "HTML";
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null && host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host;
        } catch (Exception e) {
            return url;
        }
    }

    private String extractFilename(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }
        return url;
    }

    private String generateHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }

    public CrawlStatus getCrawlStatus(String jobId) {
        return crawlJobs.get(jobId);
    }

    @lombok.Data
    public static class CrawlStatus {
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private int totalPages = 0;
        private int successPages = 0;
        private int failedPages = 0;
        private String error;

        public synchronized void incrementTotalPages() { totalPages++; }
        public synchronized void incrementSuccessPages() { successPages++; }
        public synchronized void incrementFailedPages() { failedPages++; }
    }
}