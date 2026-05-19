package com.my.rag.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.my.rag.api.document.dto.DocumentEmbeddingEstimateResponse;
import com.my.rag.api.document.dto.DocumentIndexProgressResponse;
import com.my.rag.api.document.dto.DocumentIndexResponse;
import com.my.rag.api.document.dto.DocumentStatusResponse;
import com.my.rag.api.document.dto.DocumentSummaryResponse;
import com.my.rag.api.document.dto.DocumentUploadResponse;
import com.my.rag.chunk.entity.RagDocumentChunk;
import com.my.rag.chunk.repository.RagDocumentChunkMapper;
import com.my.rag.collection.service.CollectionService;
import com.my.rag.config.RagProperties;
import com.my.rag.document.entity.RagDocument;
import com.my.rag.document.enums.DocumentStatus;
import com.my.rag.document.repository.RagDocumentMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");

    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("txt", "md", "markdown", "epub");

    private final RagDocumentMapper documentMapper;
    private final RagDocumentChunkMapper chunkMapper;
    private final RagProperties ragProperties;
    private final DocumentIndexTaskService indexTaskService;
    private final CollectionService collectionService;

    public DocumentService(
            RagDocumentMapper documentMapper,
            RagDocumentChunkMapper chunkMapper,
            RagProperties ragProperties,
            DocumentIndexTaskService indexTaskService,
            CollectionService collectionService) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.ragProperties = ragProperties;
        this.indexTaskService = indexTaskService;
        this.collectionService = collectionService;
    }

    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        return uploadDocument(file, null);
    }

    public DocumentUploadResponse uploadDocument(MultipartFile file, Long collectionId) {
        log.info("Received upload request, fileName: {}, size: {}, collectionId: {}",
                file.getOriginalFilename(), file.getSize(), collectionId);

        if (file == null || file.isEmpty()) {
            log.warn("Upload rejected: file is empty");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must not be empty");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (!StringUtils.hasText(originalFileName)) {
            log.warn("Upload rejected: file name is empty");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file name must not be empty");
        }

        String fileType = resolveFileType(originalFileName);
        if (!SUPPORTED_FILE_TYPES.contains(fileType)) {
            log.warn("Upload rejected: unsupported file type - {}", fileType);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported file type. Supported types: txt, md, markdown, epub");
        }

        byte[] bytes = readBytes(file);
        String fileHash = sha256Hex(bytes);
        Long resolvedCollectionId = collectionService.resolveUploadCollectionId(collectionId);
        RagDocument existing = findByFileHash(fileHash);
        if (existing != null) {
            log.info("Upload skipped: document already exists, documentId: {}", existing.getId());
            return toUploadResponse(existing, true);
        }

        Path savedPath = saveFile(bytes, fileHash, fileType);
        RagDocument document = new RagDocument();
        document.setTitle(resolveTitle(originalFileName));
        document.setFileName(originalFileName);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setFileHash(fileHash);
        document.setSourcePath(savedPath.toAbsolutePath().normalize().toString());
        document.setLanguage("zh");
        document.setStatus(DocumentStatus.UPLOADED);
        document.setCollectionId(resolvedCollectionId);

        documentMapper.insert(document);
        collectionService.updateCollectionCounts(resolvedCollectionId);
        log.info("Document uploaded successfully, documentId: {}, title: {}, collectionId: {}",
                document.getId(), document.getTitle(), resolvedCollectionId);
        return toUploadResponse(document, false);
    }

    public List<DocumentSummaryResponse> listDocuments() {
        log.debug("Listing all documents");
        List<DocumentSummaryResponse> result = documentMapper
                .selectList(
                        new LambdaQueryWrapper<RagDocument>()
                                .orderByDesc(RagDocument::getCreatedAt)
                                .orderByDesc(RagDocument::getId))
                .stream()
                .map(this::toSummaryResponse)
                .toList();
        log.debug("Listed {} documents", result.size());
        return result;
    }

    public DocumentStatusResponse getDocumentStatus(Long documentId) {
        log.debug("Getting document status, documentId: {}", documentId);
        RagDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            log.warn("Document not found for status query, documentId: {}", documentId);
            return new DocumentStatusResponse(documentId, "NOT_FOUND", "Document not found");
        }
        log.debug("Document status, documentId: {}, status: {}", documentId, document.getStatus());
        String errorMessage = document.getStatus() == DocumentStatus.FAILED ? document.getErrorMessage() : null;
        return new DocumentStatusResponse(
                document.getId(), document.getStatus().value(), errorMessage);
    }

    public DocumentIndexResponse indexDocument(Long documentId) {
        log.info("Received index request, documentId: {}", documentId);
        return indexTaskService.submitIndex(documentId);
    }

    public DocumentIndexResponse embedDocument(Long documentId) {
        log.info("Received embedding request, documentId: {}", documentId);
        return indexTaskService.submitEmbedding(documentId);
    }

    public DocumentEmbeddingEstimateResponse getEmbeddingEstimate(Long documentId) {
        RagDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found: " + documentId);
        }

        int chunkCount = countChunks(documentId);
        long estimatedTokens = sumEstimatedTokens(documentId);
        BigDecimal pricePer1kTokens = ragProperties.getModel().getEmbeddingPricePer1kTokens();
        if (pricePer1kTokens == null) {
            pricePer1kTokens = BigDecimal.ZERO;
        }
        BigDecimal estimatedCostCny = BigDecimal.valueOf(estimatedTokens)
                .multiply(pricePer1kTokens)
                .divide(ONE_THOUSAND, 8, RoundingMode.HALF_UP);

        return new DocumentEmbeddingEstimateResponse(
                documentId,
                chunkCount,
                estimatedTokens,
                pricePer1kTokens,
                estimatedCostCny,
                ragProperties.getModel().getEmbeddingModel(),
                ragProperties.getModel().getEmbeddingDimension());
    }

    public DocumentIndexProgressResponse getIndexProgress(Long documentId) {
        return indexTaskService.getProgress(documentId);
    }

    private DocumentSummaryResponse toSummaryResponse(RagDocument document) {
        return new DocumentSummaryResponse(
                document.getId(),
                document.getTitle(),
                document.getFileName(),
                document.getFileType(),
                document.getStatus().value(),
                document.getCollectionId());
    }

    private RagDocument findByFileHash(String fileHash) {
        return documentMapper.selectOne(
                new LambdaQueryWrapper<RagDocument>().eq(RagDocument::getFileHash, fileHash).last("LIMIT 1"));
    }

    private int countChunks(Long documentId) {
        Long count = chunkMapper.selectCount(
                new LambdaQueryWrapper<RagDocumentChunk>().eq(RagDocumentChunk::getDocumentId, documentId));
        return count == null ? 0 : Math.toIntExact(count);
    }

    private long sumEstimatedTokens(Long documentId) {
        Long tokenCount = chunkMapper.sumTokenCountByDocumentId(documentId);
        return tokenCount == null ? 0L : tokenCount;
    }

    private DocumentUploadResponse toUploadResponse(RagDocument document, boolean duplicate) {
        return new DocumentUploadResponse(
                document.getId(),
                document.getTitle(),
                document.getFileName(),
                document.getFileType(),
                document.getStatus().value(),
                duplicate);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded file", e);
        }
    }

    private Path saveFile(byte[] bytes, String fileHash, String fileType) {
        try {
            java.io.File uploadDir = new java.io.File(ragProperties.getStorage().getUploadDir());
            log.info("Upload directory configured: {}", uploadDir.getAbsolutePath());
            log.info("Upload directory exists: {}", uploadDir.exists());
            if (!uploadDir.exists()) {
                log.info("Creating upload directory...");
                uploadDir.mkdirs();
                log.info("Upload directory created successfully");
            }
            
            String shortHash = fileHash.length() > 16 ? fileHash.substring(0, 16) : fileHash;
            java.io.File savedFile = new java.io.File(uploadDir, shortHash + "." + fileType);
            log.info("Saving file to: {}", savedFile.getAbsolutePath());
            
            if (savedFile.exists()) {
                log.info("File already exists, skipping write: {}", savedFile.getAbsolutePath());
            } else {
                try {
                    java.io.File tempFile = java.io.File.createTempFile("upload-", "." + fileType, uploadDir);
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                        fos.write(bytes);
                    }
                    log.info("Temp file saved: {}", tempFile.getAbsolutePath());
                    
                    if (savedFile.exists()) {
                        log.info("Target file created by another process, deleting temp file");
                        tempFile.delete();
                    } else {
                        if (tempFile.renameTo(savedFile)) {
                            log.info("File renamed successfully: {}", savedFile.getAbsolutePath());
                        } else {
                            log.warn("Rename failed, keeping temp file: {}", tempFile.getAbsolutePath());
                            savedFile = tempFile;
                        }
                    }
                } catch (IOException e) {
                    log.warn("Temp file approach failed, trying direct write: {}", e.getMessage());
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(savedFile)) {
                        fos.write(bytes);
                    }
                }
                log.info("File saved successfully, size: {} bytes", bytes.length);
            }
            
            return savedFile.toPath();
        } catch (IOException e) {
            log.error("Failed to save uploaded file", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save uploaded file: " + e.getMessage(), e);
        }
    }

    private String resolveFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must have an extension");
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String resolveTitle(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String title = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        return StringUtils.hasText(title) ? title : fileName;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }
}
