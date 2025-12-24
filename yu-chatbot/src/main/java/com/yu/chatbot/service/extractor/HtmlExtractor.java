package com.yu.chatbot.service.extractor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class HtmlExtractor {
    
    private static final String USER_AGENT = "YU-ChatBot-Crawler/1.0";
    private static final int TIMEOUT = 10000;
    
    public String extractText(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT)
                .followRedirects(true)
                .get();
        
        doc.select("script, style, nav, footer, header, aside, .menu, .sidebar").remove();
        
        return doc.body().text();
    }
    
    public String extractTitle(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT)
                .get();
        
        return doc.title();
    }
    
    public Set<String> extractLinks(String url, String baseDomain) throws IOException {
        Set<String> links = new HashSet<>();
        
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT)
                .get();
        
        Elements linkElements = doc.select("a[href]");
        
        for (Element link : linkElements) {
            String href = link.attr("abs:href");
            
            if (href != null && !href.isEmpty() && href.contains(baseDomain)) {
                String cleanUrl = href.split("#")[0].split("\\?")[0];
                
                if (!cleanUrl.isEmpty() && !cleanUrl.startsWith("javascript:") && !cleanUrl.startsWith("mailto:")) {
                    links.add(cleanUrl);
                }
            }
        }
        
        return links;
    }
    
    public Set<String> extractFileLinks(String url) throws IOException {
        Set<String> fileLinks = new HashSet<>();
        
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT)
                .get();
        
        Elements linkElements = doc.select("a[href]");
        
        for (Element link : linkElements) {
            String href = link.attr("abs:href").toLowerCase();
            
            if (href.endsWith(".pdf") || 
                href.endsWith(".doc") || 
                href.endsWith(".docx") ||
                href.endsWith(".xls") ||
                href.endsWith(".xlsx") ||
                href.endsWith(".txt")) {
                
                fileLinks.add(link.attr("abs:href"));
            }
        }
        
        return fileLinks;
    }
}
