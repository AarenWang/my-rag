package com.my.rag.embedding.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.rag.embedding.entity.RagChunkEmbedding;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RagChunkEmbeddingMapper extends BaseMapper<RagChunkEmbedding> {

    @Delete("""
            DELETE FROM rag_chunk_embedding
            WHERE chunk_id IN (
                SELECT id FROM rag_document_chunk WHERE document_id = #{documentId}
            )
            """)
    int deleteByDocumentId(@Param("documentId") Long documentId);

    @Insert("""
            INSERT INTO rag_chunk_embedding (chunk_id, embedding, embedding_model)
            VALUES (#{chunkId}, #{embedding}::vector, #{embeddingModel})
            ON CONFLICT (chunk_id, embedding_model)
            DO UPDATE SET embedding = EXCLUDED.embedding
            """)
    int upsertEmbedding(RagChunkEmbedding embedding);
}
