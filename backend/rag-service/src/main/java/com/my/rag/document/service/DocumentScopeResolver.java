package com.my.rag.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.my.rag.collection.entity.RagCollection;
import com.my.rag.collection.repository.RagCollectionMapper;
import com.my.rag.document.entity.RagDocument;
import com.my.rag.document.enums.DocumentStatus;
import com.my.rag.document.repository.RagDocumentMapper;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentScopeResolver {

    private static final Logger log = LoggerFactory.getLogger(DocumentScopeResolver.class);

    private final RagDocumentMapper documentMapper;
    private final RagCollectionMapper collectionMapper;

    public DocumentScopeResolver(
            RagDocumentMapper documentMapper,
            RagCollectionMapper collectionMapper) {
        this.documentMapper = documentMapper;
        this.collectionMapper = collectionMapper;
    }

    public List<Long> resolveDocumentIds(List<Long> documentIds, List<Long> collectionIds) {
        if (documentIds != null && !documentIds.isEmpty()) {
            log.debug("Resolving scope using documentIds: {}", documentIds);
            return getReadyDocumentsFromIds(documentIds);
        }

        if (collectionIds != null && !collectionIds.isEmpty()) {
            log.debug("Resolving scope using collectionIds: {}", collectionIds);
            return getReadyDocumentsFromCollections(collectionIds);
        }

        log.debug("No documentIds or collectionIds provided, returning all READY documents");
        return getAllReadyDocuments();
    }

    private List<Long> getReadyDocumentsFromIds(List<Long> documentIds) {
        LambdaQueryWrapper<RagDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RagDocument::getId, documentIds);
        wrapper.eq(RagDocument::getStatus, DocumentStatus.READY);
        wrapper.select(RagDocument::getId);

        return documentMapper.selectList(wrapper).stream()
                .map(RagDocument::getId)
                .collect(Collectors.toList());
    }

    private List<Long> getReadyDocumentsFromCollections(List<Long> collectionIds) {
        LambdaQueryWrapper<RagCollection> collectionWrapper = new LambdaQueryWrapper<>();
        collectionWrapper.in(RagCollection::getId, collectionIds);
        List<RagCollection> collections = collectionMapper.selectList(collectionWrapper);

        if (collections.isEmpty()) {
            log.warn("No collections found for ids: {}", collectionIds);
            return Collections.emptyList();
        }

        LambdaQueryWrapper<RagDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RagDocument::getCollectionId, collectionIds);
        wrapper.eq(RagDocument::getStatus, DocumentStatus.READY);
        wrapper.select(RagDocument::getId);

        return documentMapper.selectList(wrapper).stream()
                .map(RagDocument::getId)
                .collect(Collectors.toList());
    }

    private List<Long> getAllReadyDocuments() {
        LambdaQueryWrapper<RagDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagDocument::getStatus, DocumentStatus.READY);
        wrapper.select(RagDocument::getId);

        return documentMapper.selectList(wrapper).stream()
                .map(RagDocument::getId)
                .collect(Collectors.toList());
    }

    public Set<Long> getReadyDocumentIdsSet(List<Long> documentIds, List<Long> collectionIds) {
        return resolveDocumentIds(documentIds, collectionIds).stream()
                .collect(Collectors.toSet());
    }
}
