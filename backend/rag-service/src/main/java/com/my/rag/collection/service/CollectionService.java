package com.my.rag.collection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.my.rag.api.collection.dto.CollectionDetailResponse;
import com.my.rag.api.collection.dto.CollectionDocumentResponse;
import com.my.rag.api.collection.dto.CollectionSummaryResponse;
import com.my.rag.api.collection.dto.CreateCollectionRequest;
import com.my.rag.api.collection.dto.UpdateCollectionRequest;
import com.my.rag.collection.entity.RagCollection;
import com.my.rag.collection.repository.RagCollectionMapper;
import com.my.rag.document.entity.RagDocument;
import com.my.rag.document.enums.DocumentStatus;
import com.my.rag.document.repository.RagDocumentMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    private final RagCollectionMapper collectionMapper;
    private final RagDocumentMapper documentMapper;

    public CollectionService(RagCollectionMapper collectionMapper, RagDocumentMapper documentMapper) {
        this.collectionMapper = collectionMapper;
        this.documentMapper = documentMapper;
    }

    public List<CollectionSummaryResponse> listCollections(Boolean includeArchived) {
        log.info("Listing collections, includeArchived: {}", includeArchived);

        LambdaQueryWrapper<RagCollection> wrapper = new LambdaQueryWrapper<>();
        if (includeArchived == null || !includeArchived) {
            wrapper.eq(RagCollection::getArchived, false);
        }
        wrapper.orderByDesc(RagCollection::getUpdatedAt);

        List<RagCollection> collections = collectionMapper.selectList(wrapper);
        return collections.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    public CollectionDetailResponse getCollectionById(Long id) {
        log.info("Getting collection by id: {}", id);

        RagCollection collection = collectionMapper.selectById(id);
        if (collection == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found");
        }

        return toDetailResponse(collection);
    }

    @Transactional
    public CollectionDetailResponse createCollection(CreateCollectionRequest request) {
        log.info("Creating collection: {}", request.name());

        RagCollection collection = new RagCollection();
        collection.setName(request.name());
        collection.setDescription(request.description());
        collection.setTags(request.tags());
        collection.setArchived(false);
        collection.setDocumentCount(0);
        collection.setReadyDocumentCount(0);
        collection.setChunkCount(0);

        collectionMapper.insert(collection);
        log.info("Collection created successfully, collectionId: {}", collection.getId());

        return toDetailResponse(collection);
    }

    @Transactional
    public CollectionDetailResponse updateCollection(Long id, UpdateCollectionRequest request) {
        log.info("Updating collection: {}", id);

        RagCollection collection = collectionMapper.selectById(id);
        if (collection == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found");
        }

        if (request.name() != null) {
            collection.setName(request.name());
        }
        if (request.description() != null) {
            collection.setDescription(request.description());
        }
        if (request.tags() != null) {
            collection.setTags(request.tags());
        }

        collectionMapper.updateById(collection);
        log.info("Collection updated successfully, collectionId: {}", id);

        return toDetailResponse(collection);
    }

    @Transactional
    public void archiveCollection(Long id) {
        log.info("Archiving collection: {}", id);

        RagCollection collection = collectionMapper.selectById(id);
        if (collection == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found");
        }

        collection.setArchived(true);
        collectionMapper.updateById(collection);
        log.info("Collection archived successfully, collectionId: {}", id);
    }

    public List<CollectionDocumentResponse> getCollectionDocuments(Long id) {
        log.info("Getting documents for collection: {}", id);

        RagCollection collection = collectionMapper.selectById(id);
        if (collection == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found");
        }

        LambdaQueryWrapper<RagDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagDocument::getCollectionId, id);
        wrapper.orderByDesc(RagDocument::getCreatedAt);

        List<RagDocument> documents = documentMapper.selectList(wrapper);
        return documents.stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    public RagCollection getDefaultCollection() {
        LambdaQueryWrapper<RagCollection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagCollection::getName, "Default");
        return collectionMapper.selectOne(wrapper);
    }

    @Transactional
    public void updateCollectionCounts(Long collectionId) {
        RagCollection collection = collectionMapper.selectById(collectionId);
        if (collection == null) {
            return;
        }

        LambdaQueryWrapper<RagDocument> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(RagDocument::getCollectionId, collectionId);
        List<RagDocument> documents = documentMapper.selectList(docWrapper);

        collection.setDocumentCount(documents.size());
        collection.setReadyDocumentCount((int) documents.stream()
                .filter(d -> d.getStatus() == DocumentStatus.READY)
                .count());

        collectionMapper.updateById(collection);
    }

    private CollectionSummaryResponse toSummaryResponse(RagCollection collection) {
        return new CollectionSummaryResponse(
                collection.getId(),
                collection.getName(),
                collection.getDescription(),
                collection.getArchived(),
                collection.getDocumentCount(),
                collection.getReadyDocumentCount(),
                collection.getChunkCount()
        );
    }

    private CollectionDetailResponse toDetailResponse(RagCollection collection) {
        return new CollectionDetailResponse(
                collection.getId(),
                collection.getName(),
                collection.getDescription(),
                collection.getTags(),
                collection.getArchived(),
                collection.getDocumentCount(),
                collection.getReadyDocumentCount(),
                collection.getChunkCount(),
                collection.getCreatedAt(),
                collection.getUpdatedAt()
        );
    }

    private CollectionDocumentResponse toDocumentResponse(RagDocument document) {
        return new CollectionDocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getFileName(),
                document.getStatus().name()
        );
    }
}
