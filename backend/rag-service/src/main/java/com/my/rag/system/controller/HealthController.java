package com.my.rag.system.controller;

import com.my.rag.common.response.ApiResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(
                Map.of(
                        "status", "UP",
                        "service", "rag-service",
                        "timestamp", Instant.now().toString()));
    }
}

