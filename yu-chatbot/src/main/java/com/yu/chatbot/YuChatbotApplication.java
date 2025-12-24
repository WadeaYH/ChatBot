package com.yu.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class YuChatbotApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(YuChatbotApplication.class, args);
    }
}
