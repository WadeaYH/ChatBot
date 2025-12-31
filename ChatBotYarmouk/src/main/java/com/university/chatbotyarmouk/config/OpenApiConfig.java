package com.university.chatbotyarmouk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(
            @Value("${spring.application.name:ChatBotYarmouk API}") String appName
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title(appName)
                        .version("v1")
                        .description("RAG-based chatbot backend for Yarmouk University"));
    }
}
