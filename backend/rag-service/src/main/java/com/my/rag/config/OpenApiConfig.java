package com.my.rag.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI myRagOpenApi() {
        return new OpenAPI()
                .info(new Info().title("My RAG API").version("0.0.1").description("Chinese ebook RAG MVP API"));
    }
}

