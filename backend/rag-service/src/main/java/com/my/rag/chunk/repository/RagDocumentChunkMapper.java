package com.my.rag.chunk.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.rag.chunk.entity.RagDocumentChunk;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RagDocumentChunkMapper extends BaseMapper<RagDocumentChunk> {
}
