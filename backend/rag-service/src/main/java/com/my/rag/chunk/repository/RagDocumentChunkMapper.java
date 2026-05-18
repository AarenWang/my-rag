package com.my.rag.chunk.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.rag.chunk.entity.RagDocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RagDocumentChunkMapper extends BaseMapper<RagDocumentChunk> {

    @Select("""
            SELECT COALESCE(SUM(token_count), 0)
            FROM rag_document_chunk
            WHERE document_id = #{documentId}
            """)
    Long sumTokenCountByDocumentId(@Param("documentId") Long documentId);
}
