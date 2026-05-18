# TODO

## 当前目标

跑通中文电子书 RAG MVP 闭环：

```text
上传电子书 -> 文本解析 -> 章节识别 -> Chunk 切分 -> Embedding -> pgvector 入库 -> 检索 -> LLM 回答 -> 来源引用
```

MVP 优先支持 `TXT`、`Markdown`、`EPUB`。暂不做复杂 Agent、复杂 PDF 版式还原、企业权限、多租户、审计日志、独立 Chat Logs 页面，以及独立 `docs/api.md` / `docs/database.md` 文档。

## 本次范围调整

- [x] 独立 `Chat Logs` 页面不作为 MVP 必需功能。
- [x] `docs/api.md` 不作为 MVP 必需文档，接口说明统一放在根目录 `README.md`。
- [x] `docs/database.md` 不作为 MVP 必需文档，数据库和配置说明统一放在根目录 `README.md`。
- [x] 保留后端 chat log 能力，用于调试和 Dashboard 最近问答展示。

## 已完成

- [x] 整理 MVP PRD/技术方案：`docs/mvp.md`
- [x] 整理工程结构和技术栈规划：`docs/engineering-structure.md`
- [x] 初始化 `.gitignore`
- [x] 初始化 PostgreSQL + pgvector 连接规划
- [x] 初始化后端 Maven 多模块：`backend/rag-common`、`backend/rag-api`、`backend/rag-service`
- [x] 初始化 Spring Boot 可启动应用 `RagApplication`
- [x] 接入基础配置：`application.yml`、`application-local.yml`
- [x] 拆分本地安全配置和机密配置：`application-local.yml` / `application-local-secret.yml`
- [x] 接入 Flyway 迁移目录和基础表结构
- [x] 新增基础接口：`/api/rag/health`、`/api/rag/documents`、`/api/rag/documents/{id}/status`、`/api/rag/chat`
- [x] 定义 RAG 主链路关键实体、DTO 和文档状态机
- [x] 验证后端 Maven 构建通过
- [x] 验证前端 Web 构建通过
- [x] 更新根目录 `README.md`

## P0 后端主链路

### 1. 本地基础设施

- [x] 准备 PostgreSQL + pgvector 服务
- [x] 配置本地 Spring Boot 数据库连接
- [x] 验证 Flyway 可成功执行 `V1__init_pgvector.sql`
- [x] 验证 Flyway 可成功创建 `rag_document`、`rag_document_chunk`、`rag_chunk_embedding`、`rag_chat_log`
- [x] 补充数据库连接、机密配置和启动说明到根目录 `README.md`

### 2. 文档领域模型

- [x] 新增 `DocumentStatus` 枚举：`UPLOADED`、`PARSING`、`PARSED`、`CHUNKING`、`CHUNKED`、`EMBEDDING`、`READY`、`FAILED`
- [x] 新增 `RagDocument` entity
- [x] 新增 `RagDocumentMapper`
- [x] 新增 `DocumentService`
- [x] 实现文档列表查询接口：`GET /api/rag/documents`
- [x] 实现文档状态查询接口：`GET /api/rag/documents/{id}/status`
- [x] 统一 `created_at`、`updated_at` 字段处理

### 3. 文件上传与去重

- [x] 实现上传接口：`POST /api/rag/documents/upload`
- [x] 支持 `TXT`、`Markdown`、`EPUB` 文件类型校验
- [x] 计算文件 SHA-256 hash
- [x] 重复上传同一文件时返回已有 `documentId`
- [x] 将原始文件保存到本地 `uploads/` 目录
- [x] 保存文件名、类型、大小、hash、source path、初始状态
- [x] 处理文件为空、格式不支持、保存失败等异常场景

### 4. 文本解析

- [x] 设计 `DocumentParser` 策略接口
- [x] 实现 `TxtDocumentParser`
- [x] 实现 `MarkdownDocumentParser`
- [x] 初步实现 `EpubDocumentParser`，基于 Tika
- [x] 保留 PDF 扩展点，复杂 PDF 放到后续阶段
- [x] 实现基础文本清洗：空行、页眉页脚噪声、重复段落、目录噪声
- [x] 文本解析为空时将文档状态置为 `FAILED`

### 5. 章节识别

- [x] 设计章节识别结果模型
- [x] 支持常见中文章节标题：`第X章`、`第X节`、`一、`、`1.`
- [x] 无法识别章节时使用默认章节
- [x] 保存章节标题到 chunk metadata

### 6. Chunk 切分

- [x] 新增 `RagDocumentChunk` entity
- [x] 新增 `RagDocumentChunkMapper`
- [x] 新增 `ChunkService`
- [x] 按章节和自然段合并生成 chunk
- [x] 支持配置 `minChars`、`maxChars`、`overlapChars`
- [x] 每个 chunk 保存 `chapterTitle`、`chunkIndex`、`startParagraph`、`endParagraph`、`contentHash`、`tokenCount`
- [x] 实现 chunk 内容 hash 去重
- [x] 实现 chunk 查询接口：`GET /api/rag/chunks?documentId=xxx`

### 7. 文档 Index 流程

- [x] 实现触发处理接口：`POST /api/rag/documents/{id}/index`
- [x] Index 流程只负责 parse + chapter recognition + chunk
- [x] 实现文档状态流转：`UPLOADED -> PARSING -> PARSED -> CHUNKING -> CHUNKED`
- [x] 任意步骤失败时状态置为 `FAILED` 并记录 `errorMessage`
- [x] MVP 先同步执行，后续再评估是否改异步任务

### 8. Embedding

- [x] 设计 `EmbeddingClient` 接口
- [x] 设计 embedding 请求/响应 DTO
- [x] 支持通过配置选择 embedding 模型
- [x] 实现云端 embedding 调用，兼容 OpenAI 风格接口
- [x] 新增 `RagChunkEmbedding` entity
- [x] 新增 `RagChunkEmbeddingMapper`
- [x] 批量生成 chunk 向量
- [x] 将向量写入 `rag_chunk_embedding`
- [x] 处理 embedding 失败、限流、空向量、维度不匹配等异常
- [x] 新增 embedding 费用预估接口：`GET /api/rag/documents/{id}/embedding/estimate`
- [x] 新增独立 embedding 执行接口：`POST /api/rag/documents/{id}/embedding`
- [x] 支持配置 `RAG_EMBEDDING_PRICE_PER_1K_TOKENS`
- [x] 文档成功 embedding 后状态流转到 `READY`

### 9. 检索

- [x] 新增 `RetrievalService`
- [x] 实现问题向量化
- [x] 实现 pgvector topK 检索 SQL
- [x] 支持按 `documentIds` 过滤
- [x] 支持 `scoreThreshold` 过滤
- [x] 返回 chunk 内容、metadata、score
- [x] 当有效召回为空时返回无依据结果

### 10. Chat 问答

- [x] 设计 `LlmClient` 接口
- [x] 设计 chat / LLM client 请求响应 DTO
- [x] 实现 LLM 调用，兼容 OpenAI 风格接口
- [x] 实现 Prompt 模板组装
- [x] 实现上下文片段格式化：`source_n`、书名、章节、chunkId、内容
- [x] 实现 `POST /api/rag/chat`
- [x] 返回 `answer`、`noAnswer`、`sources`
- [x] 回答中追加或要求包含来源引用
- [x] 召回不足时直接返回“当前资料中没有找到明确依据。”
- [x] Prompt 要求多个片段观点不一致时提示差异

### 11. 问答日志

- [x] 新增 `RagChatLog` entity
- [x] 新增 `RagChatLogMapper`
- [x] 记录 question、answer、documentIds、retrievedChunkIds、topK、minScore、latencyMs
- [x] 记录 LLM API 调用日志和失败信息
- [x] 提供 `GET /api/rag/chat/logs`
- [x] 提供 `GET /api/rag/chat/logs/{id}`
- [ ] Dashboard 接入最近问答数据

### 12. 后端异常与响应规范

- [x] 新增全局异常处理 `GlobalExceptionHandler`
- [x] 统一参数校验错误响应
- [x] 文件格式不支持返回 400
- [x] 问题为空返回 400
- [x] 无 READY 文档或无有效召回时返回明确提示
- [x] LLM 或 embedding 服务异常时返回可理解错误
- [ ] 视需要补充更明确的业务异常模型

### 13. 日志配置

- [x] 添加 `logback-spring.xml` 日志配置
- [x] 给 Controller 入口方法添加日志
- [x] 给 Service 关键方法添加日志
- [x] 记录 embedding / LLM API 调用日志

## P1 前端可视化

### 1. 前端工程初始化

- [x] 初始化 `frontend/package.json`
- [x] 初始化 `frontend/pnpm-workspace.yaml`
- [x] 初始化 `frontend/turbo.json`
- [x] 初始化 `frontend/apps/web`
- [x] 初始化 `frontend/packages/api`
- [x] 初始化 `frontend/packages/types`
- [x] 初始化 `frontend/packages/ui`
- [x] 初始化 `frontend/packages/utils`

### 2. 前端技术栈接入

- [x] 接入 Vite + React + TypeScript
- [x] 接入 React Router
- [x] 接入 TanStack Query
- [x] 接入 Ant Design
- [x] 配置 API base URL

### 3. 页面实现

- [x] 实现 `Dashboard` 页面基础统计：文档数量、READY 数量、失败数、处理中数量
- [ ] `Dashboard` 页面接入最近问答
- [x] 实现 `Documents` 页面：上传文档、查看文档列表、触发 index、查看状态
- [x] 实现 `DocumentDetail` 页面：展示 metadata、chunk 列表、处理状态、embedding 费用确认
- [x] 实现 `Chat` 页面：选择文档、输入问题、展示回答和引用来源
- [x] 实现 `Settings` 页面：展示 chunk、retrieval、model 配置
- [x] 调整 Chat 页面输入框位置：最新回答后继续提问
- [x] 独立 `Chat Logs` 页面不作为 MVP 必需页面

### 4. 前端组件

- [x] 上传能力已在 `Documents` 页面内实现，不再要求独立 `UploadPanel`
- [x] 实现 `DocumentStatusBadge`
- [x] 实现 `ChunkPreview`
- [x] 实现 `SourceList`
- [x] 实现 `ChatMessage`
- [x] 实现基础错误提示和加载状态

### 5. 前端 API Client

- [x] 封装 `GET /api/rag/documents`
- [x] 封装 `POST /api/rag/documents/upload`
- [x] 封装 `POST /api/rag/documents/{id}/index`
- [x] 封装 `GET /api/rag/documents/{id}/status`
- [x] 封装 `GET /api/rag/chunks?documentId=xxx`
- [x] 封装 `GET /api/rag/documents/{id}/embedding/estimate`
- [x] 封装 `POST /api/rag/documents/{id}/embedding`
- [x] 封装 `POST /api/rag/chat`
- [x] 封装 `GET /api/rag/chat/logs`
- [x] 封装 `GET /api/rag/chat/logs/{id}`

## P2 测试、验收与演示

### 1. 测试数据

- [ ] 准备 1 到 3 本可用于演示的中文电子书
- [ ] 优先准备 TXT 或 Markdown 格式
- [ ] 准备 10 到 20 个测试问题
- [ ] 为测试问题标注期望引用章节或片段

### 2. 后端测试

- [ ] 为文件 hash 去重补单元测试
- [x] 为文本清洗补单元测试
- [ ] 为文本解析补单元测试
- [ ] 为章节识别补单元测试
- [ ] 为 chunk 切分补单元测试
- [ ] 为无依据拒答逻辑补单元测试
- [ ] 使用 Testcontainers 验证 PostgreSQL + pgvector 迁移和基础 SQL

### 3. 集成验证

- [x] 验证后端 `mvn -pl rag-service -am test-compile` 通过
- [x] 验证前端 `pnpm --filter @my-rag/web build` 通过
- [ ] 验证文档可以从 `UPLOADED` 流转到 `READY`
- [ ] 验证至少 1 本中文电子书可完成完整入库
- [ ] 验证至少 10 个测试问题中 7 个以上能召回合理片段
- [ ] 验证无依据问题不会编造答案
- [ ] 验证每次问答都会记录 chat log
- [ ] 验证 Swagger UI 可访问

### 4. 文档补充

- [x] 更新根目录 `README.md`
- [x] 补充本地启动步骤
- [x] 补充配置和机密文件说明
- [x] 补充主要 API 列表到根目录 `README.md`
- [x] 独立 `docs/api.md` 不需要
- [x] 独立 `docs/database.md` 不需要
- [ ] 补充常见问题：Docker 未安装、数据库连接失败、模型 API 配置缺失

## P3 后续增强

- [ ] 支持复杂 PDF 版式解析
- [ ] 增加全文检索与 hybrid search
- [ ] 增加 reranker 提升中文召回质量
- [ ] 增加查询改写
- [ ] 增加多轮对话历史压缩
- [ ] 增加引用片段高亮
- [ ] 增加文档版本管理
- [ ] 增加用户反馈纠错
- [ ] 增加权限控制
- [ ] 增加审计日志
- [ ] 增加多租户
- [ ] 将文件存储从本地目录迁移到 MinIO
- [ ] 根据实际负载评估是否引入 Redis 异步任务队列
- [ ] 根据业务复杂度评估是否拆分后端服务

## 暂缓决策

- [ ] embedding 模型最终选云端还是本地
- [ ] LLM 供应商最终选 OpenAI、DeepSeek、通义还是 Claude
- [ ] 是否在 MVP 后引入 reranker
- [ ] 是否在 MVP 后引入 Redis
- [ ] 是否在 MVP 后引入 MinIO
- [ ] 是否在 MVP 后补完整权限系统
