package com.my.rag.embedding.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.rag.embedding.entity.RagEmbeddingApiLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RagEmbeddingApiLogMapper extends BaseMapper<RagEmbeddingApiLog> {
}
