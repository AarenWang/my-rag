# 中文电子书 RAG MVP 方案

## 1. 背景与目标

本项目第一阶段目标是实现一个面向中文电子书的 RAG 问答系统。用户可以上传少量中文电子书，系统完成文本解析、章节识别、语义切片、向量化、检索和基于引用片段的回答。

选择中文电子书作为 MVP 数据源有三个原因：

1. 数据来源稳定，质量通常高于随机网页内容。
2. 能完整覆盖 RAG 核心链路，适合验证工程能力。
3. 后续可平滑扩展到企业制度、产品手册、客服话术、研发文档等私有知识库场景。

MVP 的核心目标不是做复杂 Agent，而是先跑通一个可验证、可调试、可扩展的 RAG 闭环。

## 2. MVP 范围

### 2.1 本期必须支持

1. 上传 1 到 3 本中文电子书，优先支持 TXT、Markdown、EPUB。
2. 自动解析电子书文本，并识别基础章节结构。
3. 按章节和自然段生成 chunk，并保留来源 metadata。
4. 调用 embedding 模型生成向量，并写入 PostgreSQL + pgvector。
5. 用户通过接口提问，系统召回相关片段并生成回答。
6. 回答必须带引用来源，包括书名、章节、chunk 编号。
7. 当召回结果不足以支持回答时，系统应明确提示没有找到依据。
8. 提供基础接口查看文档状态、chunk 结果和问答结果。

### 2.2 本期暂不支持

1. 不做复杂 Agent 编排。
2. 不做企业级权限、多租户、审计日志。
3. 不做复杂 PDF 版式还原，PDF 支持可放到第二阶段。
4. 不做全文检索和 reranker 的完整生产实现。
5. 不做复杂前端，MVP 可以先通过 Postman 或简单页面验证。
6. 不做自动联网搜索，回答只基于已入库文档。

## 3. 用户场景

### 3.1 文档上传与入库

用户上传一本中文电子书。系统解析文本，识别章节，生成 chunk，调用 embedding 服务生成向量，最终将文档状态标记为 READY。

### 3.2 基于电子书提问

用户选择一本或多本已入库电子书并提问，例如：

```text
这本书如何解释上下文管理？
```

系统召回相关片段，将片段作为上下文提交给 LLM，并返回答案和引用来源。

### 3.3 无依据回答

如果问题在当前资料中没有明确依据，系统不应编造答案，而应返回：

```text
当前资料中没有找到明确依据。
```

## 4. 总体流程

```text
电子书文件
  ↓
文件上传与去重
  ↓
文本解析
  ↓
章节识别
  ↓
Chunk 切分
  ↓
Embedding 向量化
  ↓
写入 PostgreSQL + pgvector
  ↓
用户提问
  ↓
问题向量化
  ↓
向量召回 topK
  ↓
相似度阈值判断
  ↓
上下文组装
  ↓
LLM 生成回答
  ↓
返回 answer + sources
```

## 5. 技术方案

### 5.1 推荐技术栈

```text
后端：Java / Spring Boot
数据库：PostgreSQL
向量检索：pgvector
文本解析：Apache Tika / PDFBox / EPUB parser
Embedding：bge-m3 / text-embedding-3-large / 其他兼容模型
LLM：OpenAI / DeepSeek / 通义 / Claude 任一
接口调试：Postman / Swagger
```

MVP 建议固定主路线：

```text
Java Spring Boot + PostgreSQL pgvector + 自研 RAG Pipeline
```

原因是该路线更接近企业私有知识库工程实现，也更容易体现文档处理、检索、数据建模、异步任务和可观测性能力。

### 5.2 核心模块

```text
DocumentService：负责文件上传、文档状态管理、文件去重
ParseService：负责文本解析、章节识别、基础清洗
ChunkService：负责 chunk 切分、metadata 生成、hash 去重
EmbeddingService：负责调用 embedding 模型并保存向量
RetrievalService：负责问题向量化、pgvector topK 检索、阈值过滤
ChatService：负责上下文组装、prompt 构造、LLM 回答生成
```

## 6. 文档状态机

文档处理建议使用状态机，方便排查失败和支持异步任务。

```text
UPLOADED
  ↓
PARSING
  ↓
PARSED
  ↓
CHUNKING
  ↓
CHUNKED
  ↓
EMBEDDING
  ↓
READY
```

失败状态：

```text
FAILED
```

状态说明：

| 状态 | 说明 |
| --- | --- |
| UPLOADED | 文件已上传，尚未解析 |
| PARSING | 正在解析文本 |
| PARSED | 文本解析完成 |
| CHUNKING | 正在切片 |
| CHUNKED | chunk 已生成 |
| EMBEDDING | 正在生成向量 |
| READY | 文档可用于问答 |
| FAILED | 某一步失败，需要记录失败原因 |

## 7. Chunk 策略

中文电子书不建议直接按固定字数硬切。MVP 使用以下策略：

1. 优先按章节切分。
2. 章节内按自然段合并。
3. 每个 chunk 控制在 500 到 1000 个中文字。
4. 相邻 chunk 保留 100 到 150 字 overlap。
5. 过滤空段落、目录噪声、明显重复段落。
6. 保留书名、章节、段落范围、chunk 编号等 metadata。

chunk 示例：

```json
{
  "documentId": 1,
  "documentTitle": "示例电子书",
  "chapterTitle": "第三章 记忆与上下文",
  "chunkIndex": 27,
  "startParagraph": 12,
  "endParagraph": 16,
  "content": "……"
}
```

## 8. 数据模型

### 8.1 文档表

```sql
CREATE TABLE rag_document (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(200),
    file_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    file_hash VARCHAR(64) NOT NULL,
    source_path TEXT NOT NULL,
    language VARCHAR(50) DEFAULT 'zh',
    status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE (file_hash)
);
```

### 8.2 切片表

```sql
CREATE TABLE rag_document_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES rag_document(id),
    chapter_title VARCHAR(500),
    chunk_index INT NOT NULL,
    start_paragraph INT,
    end_paragraph INT,
    content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    token_count INT,
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE (document_id, chunk_index),
    UNIQUE (document_id, content_hash)
);

CREATE INDEX idx_rag_chunk_document_id
ON rag_document_chunk(document_id);
```

### 8.3 向量表

```sql
CREATE TABLE rag_chunk_embedding (
    id BIGSERIAL PRIMARY KEY,
    chunk_id BIGINT NOT NULL REFERENCES rag_document_chunk(id),
    embedding vector(1024) NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE (chunk_id, embedding_model)
);

CREATE INDEX idx_rag_embedding_vector
ON rag_chunk_embedding
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

说明：`vector(1024)` 需要按实际 embedding 模型维度调整。

### 8.4 问答日志表

```sql
CREATE TABLE rag_chat_log (
    id BIGSERIAL PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT,
    document_ids TEXT,
    retrieved_chunk_ids TEXT,
    top_k INT,
    min_score DOUBLE PRECISION,
    latency_ms BIGINT,
    created_at TIMESTAMP DEFAULT now()
);
```

该表用于后续调试召回质量、模型响应和性能问题。

## 9. 检索与回答策略

### 9.1 MVP 检索流程

```text
用户问题
  ↓
问题向量化
  ↓
pgvector 召回 topK = 20
  ↓
按 documentIds 过滤
  ↓
按相似度阈值过滤
  ↓
取 top 5 到 8 个 chunk 作为上下文
  ↓
LLM 基于上下文回答
```

### 9.2 阈值策略

建议配置一个最小相似度阈值 `scoreThreshold`。如果召回片段低于阈值，或者有效片段数量为 0，则不调用 LLM 生成开放答案，直接返回无依据提示。

```text
当前资料中没有找到明确依据。
```

### 9.3 第二阶段增强

第二阶段可加入：

1. 向量召回 + 关键词召回的 hybrid search。
2. bge-reranker-v2-m3 或 bge-reranker-large 重排。
3. 查询改写。
4. 多轮对话历史压缩。
5. 引用片段高亮。

## 10. Prompt 模板

```text
你是一个中文电子书知识库问答助手。
请只根据下面提供的书籍片段回答问题。

要求：
1. 如果片段中没有足够依据，请明确说“当前资料中没有找到明确依据”。
2. 不要编造书中没有出现的信息。
3. 回答后列出引用来源，包括书名、章节、chunk 编号。
4. 如果多个片段观点不同，请说明差异。
5. 回答应简洁、准确，优先总结共识，再补充差异。

用户问题：
{question}

检索片段：
{contexts}
```

上下文片段建议格式：

```text
[source_1]
书名：{documentTitle}
章节：{chapterTitle}
chunkId：{chunkId}
内容：{content}
```

## 11. API 设计

### 11.1 上传文档

```text
POST /api/rag/documents/upload
Content-Type: multipart/form-data
```

响应示例：

```json
{
  "documentId": 1,
  "title": "示例电子书",
  "status": "UPLOADED"
}
```

### 11.2 触发文档处理

```text
POST /api/rag/documents/{id}/index
```

说明：MVP 可以将解析、切片、embedding 合并为一个 index 流程。

响应示例：

```json
{
  "documentId": 1,
  "status": "EMBEDDING"
}
```

### 11.3 查询文档状态

```text
GET /api/rag/documents/{id}/status
```

响应示例：

```json
{
  "documentId": 1,
  "status": "READY",
  "errorMessage": null
}
```

### 11.4 文档列表

```text
GET /api/rag/documents
```

### 11.5 查看 chunk

```text
GET /api/rag/chunks?documentId=1
```

响应示例：

```json
{
  "documentId": 1,
  "chunks": [
    {
      "chunkId": 101,
      "chapterTitle": "第二章",
      "chunkIndex": 15,
      "startParagraph": 8,
      "endParagraph": 12,
      "contentPreview": "……"
    }
  ]
}
```

### 11.6 问答

```text
POST /api/rag/chat
Content-Type: application/json
```

请求示例：

```json
{
  "question": "这本书如何解释上下文管理？",
  "documentIds": [1, 2],
  "topK": 8,
  "scoreThreshold": 0.35
}
```

响应示例：

```json
{
  "answer": "根据检索到的片段，书中认为上下文管理的核心是……",
  "noAnswer": false,
  "sources": [
    {
      "documentId": 1,
      "documentTitle": "示例电子书",
      "chapterTitle": "第二章",
      "chunkId": 101,
      "chunkIndex": 15,
      "score": 0.82
    }
  ]
}
```

无依据响应示例：

```json
{
  "answer": "当前资料中没有找到明确依据。",
  "noAnswer": true,
  "sources": []
}
```

## 12. 配置项

```yaml
rag:
  chunk:
    min-chars: 300
    max-chars: 1000
    overlap-chars: 150
  retrieval:
    default-top-k: 20
    context-top-k: 8
    score-threshold: 0.35
  model:
    embedding-model: bge-m3
    embedding-dimension: 1024
    chat-model: deepseek-chat
```

## 13. 异常与失败处理

| 场景 | 处理方式 |
| --- | --- |
| 文件格式不支持 | 返回 400，并提示支持格式 |
| 文件重复上传 | 返回已有 documentId |
| 文本解析为空 | 文档状态置为 FAILED，记录错误原因 |
| embedding 调用失败 | 文档状态置为 FAILED，支持重试 |
| 问题为空 | 返回 400 |
| 没有 READY 文档 | 返回无可检索文档提示 |
| 召回结果低于阈值 | 返回无依据提示 |
| LLM 调用失败 | 返回模型服务异常提示，并记录日志 |

## 14. MVP 验收标准

1. 可以成功上传至少 1 本中文 TXT、Markdown 或 EPUB 电子书。
2. 文档可以从 UPLOADED 流转到 READY。
3. 系统可以生成 chunk，并能通过接口查看 chunk 内容和 metadata。
4. chunk embedding 可以成功写入 pgvector。
5. 用户提问后，系统可以返回答案和至少一个引用来源。
6. 对文档中没有依据的问题，系统不会编造答案。
7. 重复上传同一文件时可以识别并避免重复入库。
8. 至少准备 10 个测试问题，其中 7 个以上能召回到合理片段。
9. 单次问答链路应记录日志，包括问题、召回 chunk、耗时和最终回答。

## 15. 一周实现计划

### Day 1：项目骨架与数据库

1. 初始化 Spring Boot 项目。
2. 接入 PostgreSQL 和 pgvector。
3. 创建 document、chunk、embedding、chat_log 表。
4. 实现文档上传和文件 hash 去重。

### Day 2：文本解析与章节识别

1. 支持 TXT 和 Markdown 解析。
2. 初步支持 EPUB 解析。
3. 实现基础章节识别。
4. 清洗空行、目录噪声和重复段落。

### Day 3：Chunk 切分

1. 实现按章节和自然段合并的 chunk 策略。
2. 保存 chunk metadata。
3. 提供 chunk 查询接口。

### Day 4：Embedding 入库

1. 接入 embedding 模型。
2. 批量生成 chunk 向量。
3. 写入 pgvector。
4. 完成文档状态流转。

### Day 5：检索与问答

1. 实现问题向量化。
2. 实现 pgvector topK 检索。
3. 实现 scoreThreshold 过滤。
4. 接入 LLM 并返回 answer + sources。

### Day 6：测试与调参

1. 准备 10 到 20 个测试问题。
2. 调整 chunk 大小、overlap 和 topK。
3. 验证无依据问题不会被编造回答。
4. 记录典型问题和召回结果。

### Day 7：整理与演示

1. 补充 README 使用说明。
2. 整理 API 示例。
3. 准备一组可演示电子书和问题。
4. 记录当前限制和下一阶段计划。

## 16. 后续规划

MVP 跑通后，可逐步扩展为企业私有知识库 RAG：

1. 增加 PDF 复杂版式解析。
2. 增加全文检索与 hybrid search。
3. 增加 reranker 提升中文召回质量。
4. 增加文档版本管理。
5. 增加用户反馈纠错。
6. 增加权限控制、审计日志和多租户。
7. 增加前端页面，支持文档管理、问答和引用片段预览。

## 17. 简历表达参考

项目名称：

```text
中文电子书知识库 RAG 系统 / 企业知识库 RAG 原型系统
```

项目描述：

```text
基于中文电子书构建知识库问答系统，实现从文档解析、章节识别、Chunk 切分、Embedding 向量化、pgvector 检索、上下文组装到 LLM 问答的完整 RAG 流程，并支持答案引用溯源与低置信度拒答。系统可扩展至企业制度、产品文档、客服知识库等私有知识库场景。
```

核心职责：

```text
1. 设计文档解析与切片 Pipeline，支持 TXT / Markdown / EPUB 等中文电子书格式。
2. 基于章节与自然段实现语义化 chunk 切分，保留书名、章节、段落编号等 metadata。
3. 使用 embedding 模型生成文本向量，并写入 PostgreSQL pgvector。
4. 实现向量检索与上下文组装，根据 topK 片段生成 grounded answer。
5. 设计回答引用和低置信度拒答机制，降低幻觉风险。
6. 预留 reranker、全文检索、权限控制和企业知识库扩展能力。
```
