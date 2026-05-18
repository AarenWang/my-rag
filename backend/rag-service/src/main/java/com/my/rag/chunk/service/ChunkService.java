package com.my.rag.chunk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.my.rag.chunk.entity.RagDocumentChunk;
import com.my.rag.chunk.repository.RagDocumentChunkMapper;
import com.my.rag.config.RagProperties;
import com.my.rag.embedding.repository.RagChunkEmbeddingMapper;
import com.my.rag.parser.dto.Chapter;
import com.my.rag.parser.dto.ParsedDocument;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChunkService {

    private static final Logger log = LoggerFactory.getLogger(ChunkService.class);

    private final RagDocumentChunkMapper chunkMapper;
    private final RagChunkEmbeddingMapper embeddingMapper;
    private final RagProperties ragProperties;

    public ChunkService(
            RagDocumentChunkMapper chunkMapper,
            RagChunkEmbeddingMapper embeddingMapper,
            RagProperties ragProperties) {
        this.chunkMapper = chunkMapper;
        this.embeddingMapper = embeddingMapper;
        this.ragProperties = ragProperties;
    }

    @Transactional
    public int createChunks(Long documentId, ParsedDocument parsedDocument, List<Chapter> chapters) {
        log.info("Creating chunks for documentId: {}, chapters: {}", documentId, chapters.size());
        deleteChunksByDocumentId(documentId);

        Set<String> existingHashes = new HashSet<>();
        int chunkIndex = 0;
        int totalCreated = 0;

        for (Chapter chapter : chapters) {
            log.debug("Processing chapter: {}, paragraphs: {}", chapter.title(), chapter.contentParagraphs().size());
            ChunkCreateResult result = createChapterChunks(
                    documentId,
                    chapter,
                    chunkIndex,
                    existingHashes
            );
            chunkIndex = result.nextChunkIndex();
            totalCreated += result.createdCount();
            log.debug("Chapter processed, created {} chunks", result.createdCount());
        }

        log.info("Chunks created successfully for documentId: {}, total: {}", documentId, totalCreated);
        return totalCreated;
    }

    public List<RagDocumentChunk> getChunksByDocumentId(Long documentId) {
        return chunkMapper.selectList(
                new LambdaQueryWrapper<RagDocumentChunk>()
                        .eq(RagDocumentChunk::getDocumentId, documentId)
                        .orderByAsc(RagDocumentChunk::getChunkIndex)
        );
    }

    private void deleteChunksByDocumentId(Long documentId) {
        embeddingMapper.deleteByDocumentId(documentId);
        chunkMapper.delete(
                new LambdaQueryWrapper<RagDocumentChunk>()
                        .eq(RagDocumentChunk::getDocumentId, documentId)
        );
    }

    private ChunkCreateResult createChapterChunks(
            Long documentId,
            Chapter chapter,
            int startChunkIndex,
            Set<String> existingHashes
    ) {
        List<String> paragraphs = chapter.contentParagraphs();
        int minChars = ragProperties.getChunk().getMinChars();
        int maxChars = ragProperties.getChunk().getMaxChars();
        int overlapChars = ragProperties.getChunk().getOverlapChars();

        int currentStartParagraph = 0;
        int chunkIndex = startChunkIndex;
        int createdCount = 0;

        while (currentStartParagraph < paragraphs.size()) {
            List<String> currentParagraphs = new ArrayList<>();
            int currentLength = 0;
            int currentEndParagraph = currentStartParagraph;

            while (currentEndParagraph < paragraphs.size()) {
                String nextParagraph = paragraphs.get(currentEndParagraph);
                int addLength = (currentLength > 0 ? 2 : 0) + nextParagraph.length();

                if (currentLength + addLength > maxChars) {
                    if (currentLength >= minChars) {
                        break;
                    }
                    if (currentParagraphs.isEmpty()) {
                        currentParagraphs.add(nextParagraph);
                        currentLength += nextParagraph.length();
                        currentEndParagraph++;
                    }
                    break;
                }

                currentParagraphs.add(nextParagraph);
                currentLength += addLength;
                currentEndParagraph++;
            }

            if (!currentParagraphs.isEmpty()) {
                String content = String.join("\n\n", currentParagraphs);
                String contentHash = sha256Hex(content);

                if (existingHashes.add(contentHash)) {
                    RagDocumentChunk chunk = new RagDocumentChunk();
                    chunk.setDocumentId(documentId);
                    chunk.setChapterTitle(chapter.title());
                    chunk.setChunkIndex(chunkIndex);
                    chunk.setStartParagraph(chapter.startParagraph() + currentStartParagraph);
                    chunk.setEndParagraph(chapter.startParagraph() + currentEndParagraph - 1);
                    chunk.setContent(content);
                    chunk.setContentHash(contentHash);
                    chunk.setTokenCount(estimateTokenCount(content));
                    chunkMapper.insert(chunk);
                    chunkIndex++;
                    createdCount++;
                }
            }

            if (currentEndParagraph >= paragraphs.size()) {
                break;
            }

            int nextStartParagraph = findOverlapStart(paragraphs, currentEndParagraph, overlapChars);
            currentStartParagraph = nextStartParagraph <= currentStartParagraph
                    ? currentStartParagraph + 1
                    : nextStartParagraph;
        }

        return new ChunkCreateResult(chunkIndex, createdCount);
    }

    private int findOverlapStart(List<String> paragraphs, int currentEnd, int overlapChars) {
        int start = Math.max(0, currentEnd - 1);
        int accumulatedChars = 0;

        while (start > 0 && accumulatedChars < overlapChars) {
            String para = paragraphs.get(start - 1);
            if (accumulatedChars + para.length() > overlapChars) {
                break;
            }
            accumulatedChars += para.length() + 2;
            start--;
        }

        return start;
    }

    private int estimateTokenCount(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return (int) Math.ceil(content.length() / 1.5);
    }

    private String sha256Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }

    private record ChunkCreateResult(int nextChunkIndex, int createdCount) {}
}
