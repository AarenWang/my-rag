package com.my.rag.collection.controller;

import com.my.rag.api.collection.dto.CollectionDetailResponse;
import com.my.rag.api.collection.dto.CollectionDocumentResponse;
import com.my.rag.api.collection.dto.CollectionSummaryResponse;
import com.my.rag.api.collection.dto.CreateCollectionRequest;
import com.my.rag.api.collection.dto.UpdateCollectionRequest;
import com.my.rag.collection.service.CollectionService;
import com.my.rag.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag/collections")
public class CollectionController {

    private static final Logger log = LoggerFactory.getLogger(CollectionController.class);

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping
    public ApiResponse<List<CollectionSummaryResponse>> listCollections(
            @RequestParam(required = false) Boolean includeArchived) {
        log.info("API request: GET /api/rag/collections, includeArchived: {}", includeArchived);
        return ApiResponse.success(collectionService.listCollections(includeArchived));
    }

    @PostMapping
    public ApiResponse<CollectionDetailResponse> createCollection(@Valid @RequestBody CreateCollectionRequest request) {
        log.info("API request: POST /api/rag/collections, name: {}", request.name());
        return ApiResponse.success(collectionService.createCollection(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<CollectionDetailResponse> getCollection(@PathVariable Long id) {
        log.info("API request: GET /api/rag/collections/{}", id);
        return ApiResponse.success(collectionService.getCollectionById(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<CollectionDetailResponse> updateCollection(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCollectionRequest request) {
        log.info("API request: PATCH /api/rag/collections/{}", id);
        return ApiResponse.success(collectionService.updateCollection(id, request));
    }

    @PostMapping("/{id}/archive")
    public ApiResponse<Void> archiveCollection(@PathVariable Long id) {
        log.info("API request: POST /api/rag/collections/{}/archive", id);
        collectionService.archiveCollection(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/documents")
    public ApiResponse<List<CollectionDocumentResponse>> getCollectionDocuments(@PathVariable Long id) {
        log.info("API request: GET /api/rag/collections/{}/documents", id);
        return ApiResponse.success(collectionService.getCollectionDocuments(id));
    }
}
