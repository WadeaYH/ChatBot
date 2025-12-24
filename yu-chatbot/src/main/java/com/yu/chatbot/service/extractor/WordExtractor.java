package com.yu.chatbot.service.extractor;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class WordExtractor {
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    public String extractText(String docUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(docUrl))
                .header("User-Agent", "YU-ChatBot-Crawler/1.0")
                .GET()
                .build();
        
        HttpResponse<InputStream> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );
        
        return extractFromStream(response.body());
    }
    
    public String extractFromStream(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append(" ");
                }
            }
            
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.trim().isEmpty()) {
                            content.append(cellText).append(" ");
                        }
                    }
                }
            }
        }
        
        return content.toString().replaceAll("\\s+", " ").trim();
    }
}
