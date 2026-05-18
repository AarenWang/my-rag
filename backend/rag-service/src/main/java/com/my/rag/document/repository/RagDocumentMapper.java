package com.my.rag.document.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.rag.document.entity.RagDocument;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RagDocumentMapper extends BaseMapper<RagDocument> {}

