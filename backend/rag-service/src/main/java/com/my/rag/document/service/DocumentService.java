package com.my.rag.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class DocumentService {

    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("txt", "md", "markdown", "epub");

    private final RagDocumentMapper documentMapper;
    private final RagProperties ragProperties;

    public DocumentService(RagDocumentMapper documentMapper, RagProperties ragProperties) {
        this.documentMapper = documentMapper;
        this.ragProperties = ragProperties;
    }

    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must not be empty");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (!StringUtils.hasText(originalFileName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file name must not be empty");
        }

        String fileType = resolveFileType(originalFileName);
        if (!SUPPORTED_FILE_TYPES.contains(fileType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported file type. Supported types: txt, md, markdown, epub");
        }

        byte[] bytes = readBytes(file);
        String fileHash = sha256Hex(bytes);
        RagDocument existing = findByFileHash(fileHash);
        if (existing != null) {
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
        return toUploadResponse(document, false);
    }

    public List<DocumentSummaryResponse> listDocuments() {
        return documentMapper
                .selectList(
                        new LambdaQueryWrapper<RagDocument>()
                                .orderByDesc(RagDocument::getCreatedAt)
                                .orderByDesc(RagDocument::getId))
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public DocumentStatusResponse getDocumentStatus(Long documentId) {
        RagDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            return new DocumentStatusResponse(documentId, "NOT_FOUND", "Document not found");
        }
        return new DocumentStatusResponse(
                document.getId(), document.getStatus().value(), document.getErrorMessage());
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
