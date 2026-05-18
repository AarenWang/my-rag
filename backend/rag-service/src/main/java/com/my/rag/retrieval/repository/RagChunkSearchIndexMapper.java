package com.my.rag.retrieval.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.rag.retrieval.entity.RagChunkSearchIndex;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RagChunkSearchIndexMapper extends BaseMapper<RagChunkSearchIndex> {

    @Delete("DELETE FROM rag_chunk_search_index WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    @Insert("""
            INSERT INTO rag_chunk_search_index (chunk_id, document_id, search_text, search_vector)
            VALUES (
                #{chunkId},
                #{documentId},
                #{searchText},
                to_tsvector(CAST(#{textSearchConfig} AS regconfig), #{searchText})
            )
            ON CONFLICT (chunk_id)
            DO UPDATE SET
                document_id = EXCLUDED.document_id,
                search_text = EXCLUDED.search_text,
                search_vector = EXCLUDED.search_vector,
                updated_at = now()
            """)
    int upsertSearchIndex(RagChunkSearchIndex searchIndex);
}
