package com.yu.chatbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documents")
public class WebDocument {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String url;
    
    private String title;
    
    @TextIndexed
    private String content;
    
    private String fileType;
    
    private LocalDateTime crawledAt;
    
    private String contentHash;
    
    private String status;
    
    private String parentUrl;
    
    private int depth;
}
