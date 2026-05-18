package com.my.rag.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.my.rag.api.document.dto.DocumentIndexProgressResponse;
import com.my.rag.api.document.dto.DocumentIndexResponse;
import com.my.rag.api.document.dto.DocumentStatusResponse;
import com.my.rag.api.document.dto.DocumentSummaryResponse;
import com.my.rag.api.document.dto.DocumentUploadResponse;
import com.my.rag.config.RagProperties;
import com.my.rag.document.entity.RagDocument;
import com.my.rag.document.enums.DocumentStatus;
import com.my.rag.document.repository.RagDocumentMapper;
import java.io.IOException;
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

    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("txt", "md", "markdown", "epub");

    private final RagDocumentMapper documentMapper;
    private final RagProperties ragProperties;
    private final DocumentIndexTaskService indexTaskService;

    public DocumentService(
            RagDocumentMapper documentMapper,
            RagProperties ragProperties,
            DocumentIndexTaskService indexTaskService) {
        this.documentMapper = documentMapper;
        this.ragProperties = ragProperties;
        this.indexTaskService = indexTaskService;
    }

    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        log.info("Received upload request, fileName: {}, size: {}", file.getOriginalFilename(), file.getSize());
        
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

        documentMapper.insert(document);
        log.info("Document uploaded successfully, documentId: {}, title: {}", document.getId(), document.getTitle());
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
        return indexTaskService.submit(documentId);
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
                document.getStatus().value());
    }

    private RagDocument findByFileHash(String fileHash) {
        return documentMapper.selectOne(
                new LambdaQueryWrapper<RagDocument>().eq(RagDocument::getFileHash, fileHash).last("LIMIT 1"));
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
            Path uploadDir = Path.of(ragProperties.getStorage().getUploadDir());
            Files.createDirectories(uploadDir);
            Path savedPath = uploadDir.resolve(fileHash + "." + fileType);
            Files.write(savedPath, bytes);
            return savedPath;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save uploaded file", e);
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
