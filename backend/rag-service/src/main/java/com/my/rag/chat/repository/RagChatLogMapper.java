package com.my.rag.chat.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.rag.chat.entity.RagChatLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RagChatLogMapper extends BaseMapper<RagChatLog> {
}
