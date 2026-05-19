# RAG 核心功能路线图

本文记录项目后续可添加的核心功能。范围限定为企业级 RAG 知识问答内核和个人知识助手能力，暂不考虑企业用户登录、企业权限、多租户、审计日志和复杂数据来源集成。

## 当前基础

项目当前已经具备 RAG MVP 主链路：

- 文档上传、去重和状态管理。
- 文档解析、章节识别、chunk 切分。
- embedding 费用预估和向量入库。
- pgvector 相似度检索。
- keyword search、RRF 融合、reranker 扩展点、EvidencePack、ContextBuilder。
- 基于引用片段的问答。
- Chat log、API 调用日志和基础前端页面。

后续重点不是继续堆数据源或权限系统，而是提升 RAG 的可解释性、准确率、可运营性和个人知识沉淀能力。

## P0：RAG 核心质量能力

### 1. 检索调试工作台

新增用于排查问答质量的调试页面和 API。

建议能力：

- 输入问题并选择文档或知识库。
- 展示向量召回结果。
- 展示关键词召回结果。
- 展示 RRF 融合后排序。
- 展示 reranker 后排序。
- 展示最终进入 prompt 的 EvidencePack。
- 展示每个 chunk 的 score、rank、召回来源、章节、内容。
- 支持将一次调试结果保存为评测样例。

价值：

- 判断回答错误到底来自未召回、排序不准、上下文不足、prompt 问题还是模型生成问题。
- 为后续调 chunk、reranker、query rewrite 和 prompt 提供依据。

### 2. 真实 Reranker 接入

当前代码已有 `RerankerClient` 和 `NoopRerankerClient`，下一步应接入真实 reranker。

建议支持配置：

- `noop`
- `dashscope-rerank`
- `bge-reranker`
- `openai-compatible-rerank`

目标流程：

```text
vector recall + keyword recall
  -> RRF
  -> reranker
  -> context builder
  -> LLM
```

价值：

- 改善“召回到了但排序不准”的问题。
- 提升最终进入 prompt 的证据质量。

### 3. Query Rewrite / Query Expansion

用户问题通常不适合直接检索，建议增加查询改写层。

建议生成：

- 原问题。
- 关键词查询。
- 同义改写。
- 子问题拆分。
- 中英文术语扩展。

示例：

```text
用户问题：
这个系统怎么控制 embedding 成本？

扩展查询：
embedding 成本
费用预估
token count
price per 1k tokens
文档向量化费用
```

价值：

- 提升中文、技术文档、长问题和模糊问题的召回率。
- 为混合检索提供更稳定的关键词输入。

### 4. 答案引用强绑定

当前回答会返回 sources，后续可以升级为结论和引用强绑定。

建议响应结构：

```json
{
  "answer": "...",
  "claims": [
    {
      "text": "系统会先根据 chunk tokenCount 估算 embedding 成本。",
      "sourceIds": ["source_1", "source_3"]
    }
  ],
  "sources": []
}
```

前端能力：

- 鼠标悬浮一句话时显示引用片段。
- 点击引用跳到 chunk 原文。
- 高亮没有引用支撑的结论。
- 支持只看引用内容。

价值：

- 提升回答可信度。
- 便于用户核对答案是否真的来自知识库。

### 5. 拒答策略增强

当前已有无依据回答，后续可细化拒答原因。

建议区分：

- 没有召回到内容。
- 召回内容相关但不足以回答。
- 多个来源冲突。
- 问题超出知识库范围。
- 用户要求推测、总结或建议，但资料不足。

价值：

- 前端可以给出更准确的提示。
- 日志和评测可以统计不同拒答类型。

## P1：知识组织能力

### 1. 知识库 / Collection

在文档之上增加知识库组织层。

建议模型：

```text
Collection
  -> Documents
    -> Chunks
```

个人场景：

- 读书。
- 工作资料。
- 论文。
- 产品文档。
- 项目笔记。

企业场景：

- 产品知识库。
- 客服知识库。
- 研发规范。
- 运维手册。
- 销售资料。

价值：

- 文档数量变多后，按单个文档选择会很难用。
- Collection 是后续权限、分享、评测和统计的自然边界。

### 2. 标签和元数据

给文档增加元数据：

- 标签。
- 描述。
- 作者。
- 来源。
- 语言。
- 创建时间。
- 更新时间。
- 文档类型。
- 重要程度。
- 是否归档。

给 chunk 增加元数据：

- 章节路径。
- 页码。
- 标题层级。
- token 数。
- hash。
- 质量评分。

价值：

- 支持按标签、时间、类型过滤检索。
- 为索引质量分析和文档运营提供基础。

### 3. 文档版本

同一文档重新上传时，不应简单覆盖。

建议模型：

```text
document_group
  -> document_version_1
  -> document_version_2
```

建议能力：

- 查看当前版本。
- 查看历史版本。
- 重新索引某个版本。
- 问答时指定只使用最新版。
- 回答中显示来源版本。

价值：

- 避免答案引用过期制度、手册或接口文档。
- 支持文档更新后的回溯和对比。

## P2：个人知识助手能力

### 1. 问答转笔记

用户看到回答后，可以保存为笔记。

笔记内容：

- 问题。
- 回答。
- 引用来源。
- 用户备注。
- 标签。
- 所属知识库。

价值：

- 让系统从搜索工具变成知识沉淀工具。

### 2. 摘录和高亮

在 chunk 或原文视图中支持：

- 高亮一段内容。
- 添加批注。
- 保存摘录。
- 从摘录发起追问。

价值：

- 适合读书、论文阅读和研究资料整理。

### 3. 文档总结

对单个文档提供：

- 总摘要。
- 分章节摘要。
- 关键概念。
- 人物、术语、实体列表。
- FAQ。
- 行动项。
- 重点摘录。

对多个文档提供：

- 对比总结。
- 观点冲突。
- 共识整理。
- 时间线。
- 主题聚类。

价值：

- 提升个人知识助手的主动整理能力。
- 复用已有解析、检索和 LLM 调用链路。

### 4. 学习卡片

从文档生成学习卡片。

示例：

```text
问题：RAG 为什么需要 reranker？
答案：因为初始召回只负责找候选，reranker 负责判断候选是否真正能回答问题。
来源：source_2
```

卡片类型：

- 问答卡。
- 概念卡。
- 术语卡。
- 摘录卡。

价值：

- 适合个人学习和复习场景。

### 5. 多轮会话

新增 conversation 和 message 模型。

建议模型：

```text
conversation
  -> messages
  -> retrieval snapshots
  -> sources
```

关键要求：

- 多轮追问不能只依赖历史回答。
- 应结合历史意图改写当前问题，然后重新检索。

示例：

```text
第一轮：这本书怎么看上下文工程？
第二轮：它和记忆有什么区别？
```

第二轮应自动改写为：

```text
根据上一轮讨论的上下文工程，它和记忆有什么区别？
```

然后重新召回。

## P3：生产可用性能力

### 1. 索引质量报告

文档入库后生成质量报告。

建议指标：

- 文档解析是否成功。
- 识别出多少章节。
- 生成多少 chunk。
- chunk 平均长度。
- 过短 chunk 数量。
- 过长 chunk 数量。
- 重复 chunk 数量。
- embedding 成功率。
- keyword index 是否生成。
- 可能的解析噪声。

价值：

- 判断文档是否适合问答。
- 方便定位解析、切片和索引问题。

### 2. 任务队列持久化

当前异步任务进度主要在内存里，建议增加任务表。

建议表：

```text
rag_index_task
```

建议字段：

- taskId。
- documentId。
- taskType。
- status。
- stage。
- progress。
- errorMessage。
- startedAt。
- finishedAt。

价值：

- 服务重启后任务状态不丢。
- 前端可以查看历史任务。
- 后续可以支持取消、重试和批量任务。

### 3. 成本统计

在已有 embedding 费用预估基础上继续扩展。

建议统计：

- 每个文档 embedding 成本。
- 每次问答 LLM token 成本。
- 每天总成本。
- 每个模型调用次数。
- 平均 latency。
- 失败率。

价值：

- 方便个人用户控制成本。
- 为企业级部署提供运营指标。

### 4. 模型配置测试

Settings 页面增加模型连接测试。

建议能力：

- 测试 embedding。
- 测试 chat。
- 测试 reranker。
- 检查 embedding dimension。
- 检查 pgvector 维度是否匹配。
- 检查 keyword search config 是否可用。

价值：

- 降低本地配置和模型接入的排查成本。

## P4：评测与反馈

### 1. RAG 评测集

新增评测样例模型。

建议字段：

```text
question
expectedAnswer
expectedSourceChunkIds
expectedNoAnswer
documentIds
```

建议指标：

- recall hit rate。
- source precision。
- no-answer accuracy。
- answer groundedness。
- latency。
- token cost。

价值：

- 后续改 chunk、prompt、reranker、query rewrite 时可以做回归测试。
- 避免靠主观感觉调参。

### 2. 用户反馈

每个回答支持反馈。

反馈类型：

- 有用。
- 没用。
- 引用不对。
- 答案不完整。
- 应该拒答。
- 文档缺失。

价值：

- 将真实使用问题沉淀为优化线索。
- 后续可转化为评测样例。

### 3. 错误样例复盘

对失败问答保存完整链路。

建议保存：

- question。
- rewritten queries。
- retrieved chunks。
- reranked chunks。
- evidence pack。
- prompt。
- answer。
- feedback。

价值：

- 支持持续优化 RAG 系统。
- 方便定位失败原因。

## 推荐实施顺序

建议后续按以下顺序推进：

1. 检索调试 API + 前端页面。
2. 真实 reranker 接入。
3. Query rewrite / keyword expansion。
4. Collection 知识库模型。
5. 多轮会话。
6. 答案 claim-source 绑定。
7. 索引质量报告。
8. 评测集和一键评测。
9. 笔记、摘录、总结、学习卡片。
10. 任务持久化和成本统计。

如果只选最值得马上做的三个：

```text
1. 检索调试工作台
2. 真实 reranker
3. Query rewrite / 多路召回
```

这三项会直接提升 RAG 回答质量，并让后续所有优化都有可观测依据。

## 需要先细化设计的功能

以下功能会影响数据模型、接口契约、前端工作流或后续扩展边界，不建议直接边写边定。

### 1. Collection 知识库模型

#### 目标

在 Document 之上增加知识库组织层，让用户可以按主题、用途或项目管理文档。第一版不做权限和多租户，但数据模型要为后续权限边界预留。

#### 范围决策

- 第一版采用 `Collection -> Documents` 的一对多关系。
- 一个文档只属于一个 Collection，避免检索过滤、统计和前端选择复杂化。
- 提供一个默认 Collection，兼容已有未分组文档。
- 检索接口可以接受 `collectionIds`，后端展开为 READY documents 后继续复用现有 documentIds 检索链路。
- 暂不做跨 Collection 去重，重复文档仍由当前 file hash 去重规则控制。

#### 数据模型

新增表：

```sql
CREATE TABLE rag_collection (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    tags TEXT,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    document_count INT NOT NULL DEFAULT 0,
    ready_document_count INT NOT NULL DEFAULT 0,
    chunk_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);
```

修改 `rag_document`：

```sql
ALTER TABLE rag_document
ADD COLUMN collection_id BIGINT REFERENCES rag_collection(id);

CREATE INDEX idx_rag_document_collection_id
ON rag_document(collection_id);
```

迁移策略：

1. 创建默认 Collection，例如 `Default`。
2. 将已有文档的 `collection_id` 指向默认 Collection。
3. 后续上传接口未传 `collectionId` 时自动写入默认 Collection。

#### 后端接口

新增：

```text
GET    /api/rag/collections
POST   /api/rag/collections
GET    /api/rag/collections/{id}
PATCH  /api/rag/collections/{id}
POST   /api/rag/collections/{id}/archive
GET    /api/rag/collections/{id}/documents
```

调整：

```text
POST /api/rag/documents/upload
```

支持 multipart 参数：

```text
collectionId
```

调整 Chat 和 Retrieval Debug 请求：

```json
{
  "question": "...",
  "collectionIds": [1, 2],
  "documentIds": [10, 11]
}
```

规则：

- `documentIds` 优先级高于 `collectionIds`。
- 如果两者都为空，则默认检索所有 READY 文档。
- 如果只传 `collectionIds`，后端查询这些 Collection 下的 READY 文档并展开。

#### 后端实现

新增模块：

```text
collection/entity/RagCollection.java
collection/repository/RagCollectionMapper.java
collection/service/CollectionService.java
collection/controller/CollectionController.java
```

新增辅助服务：

```text
DocumentScopeResolver
```

职责：

- 统一解析 `documentIds` 和 `collectionIds`。
- 只返回 READY 文档 ID。
- 被 Chat、Retrieval、RetrievalDebug 复用。

#### 前端实现

新增页面：

```text
/collections
/collections/:id
```

改造：

- Documents 页面支持按 Collection 过滤和上传时选择 Collection。
- Chat 页面支持选择 Collection，保留选择具体文档的高级入口。
- Retrieval Debug 页面支持按 Collection 调试。
- Dashboard 增加 Collection 统计。

#### 验收标准

- 老数据自动进入默认 Collection。
- 上传文档时可以选择 Collection。
- Chat 和 Retrieval Debug 可以按 Collection 限定检索范围。
- 不选择 Collection 时行为与当前版本一致。
- 归档 Collection 后默认列表隐藏，但历史文档和日志不丢失。

### 2. 多轮会话

#### 目标

把当前单轮 Chat 升级为可追问的 conversation。每轮回答仍然重新检索，避免只依赖历史回答导致幻觉。

#### 数据模型

新增表：

```sql
CREATE TABLE rag_conversation (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(300),
    collection_ids TEXT,
    document_ids TEXT,
    message_count INT NOT NULL DEFAULT 0,
    last_message_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE rag_conversation_message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES rag_conversation(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    no_answer BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_rag_conversation_message_conversation_id
ON rag_conversation_message(conversation_id, id);
```

新增检索快照表：

```sql
CREATE TABLE rag_retrieval_snapshot (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT REFERENCES rag_conversation(id) ON DELETE SET NULL,
    message_id BIGINT REFERENCES rag_conversation_message(id) ON DELETE SET NULL,
    question TEXT NOT NULL,
    rewritten_question TEXT,
    document_ids TEXT,
    vector_chunk_ids TEXT,
    keyword_chunk_ids TEXT,
    rrf_chunk_ids TEXT,
    reranked_chunk_ids TEXT,
    evidence_json TEXT,
    prompt_context TEXT,
    created_at TIMESTAMP DEFAULT now()
);
```

#### 查询改写策略

新增：

```text
ConversationQueryRewriteService
```

输入：

- 当前用户问题。
- 最近 N 轮消息。
- 当前会话 documentIds / collectionIds。

输出：

```json
{
  "standaloneQuestion": "根据上一轮讨论的上下文工程，它和记忆有什么区别？",
  "reason": "用户使用了代词“它”，需要指代上一轮主题。"
}
```

第一版可使用规则优先：

- 如果问题包含“它/这个/上面/刚才/前面/两者”，拼接上一轮用户问题和助手回答摘要。
- 如果问题本身完整，则原样使用。

第二版再接 LLM query rewrite。

#### 接口设计

新增：

```text
POST /api/rag/conversations
GET  /api/rag/conversations
GET  /api/rag/conversations/{id}
POST /api/rag/conversations/{id}/messages
```

发送消息请求：

```json
{
  "question": "它和记忆有什么区别？",
  "collectionIds": [1],
  "documentIds": [],
  "topK": 8,
  "scoreThreshold": 0.35
}
```

响应：

```json
{
  "messageId": 100,
  "answer": "...",
  "noAnswer": false,
  "rewrittenQuestion": "...",
  "sources": [],
  "snapshotId": 200
}
```

#### Prompt 组装策略

- 检索使用 `rewrittenQuestion`。
- LLM 回答 prompt 中包含原问题、改写问题和 EvidencePack。
- 历史消息只用于理解追问，不直接作为事实依据。
- 历史消息最多保留最近 6 条，超过后用摘要替代。

#### 前端实现

新增：

- 会话列表。
- 会话详情。
- 新建会话。
- 追问输入框。
- 展示 `rewrittenQuestion` 的调试折叠区。

Chat 页面调整为：

- 默认进入最近会话。
- 可以新建临时会话。
- 每轮消息下展示 sources。

#### 验收标准

- 单轮问答能力不回退。
- 追问能继承上一轮主题并重新检索。
- 每轮回答都有独立 sources 和 retrieval snapshot。
- 会话刷新后历史消息仍可恢复。

### 3. Claim-source 结构化回答

#### 目标

把回答拆成可核验的结论，并把每条结论绑定到 sourceId。第一版采用要点级 claim，而不是句子级，降低解析难度。

#### 响应结构

新增 DTO：

```json
{
  "answer": "整体回答文本",
  "noAnswer": false,
  "claims": [
    {
      "text": "系统会先根据 chunk tokenCount 估算 embedding 成本。",
      "sourceIds": ["source_1", "source_3"],
      "confidence": "high"
    }
  ],
  "sources": []
}
```

`confidence` 第一版只允许：

```text
high
medium
low
```

#### LLM 输出约束

Prompt 要求模型输出 JSON：

```json
{
  "answer": "...",
  "claims": [
    {
      "text": "...",
      "sourceIds": ["source_1"]
    }
  ],
  "noAnswer": false
}
```

规则：

- 每个 claim 至少绑定一个 sourceId。
- 如果没有足够依据，`noAnswer=true`，claims 为空。
- 不允许引用不存在的 sourceId。
- 如果多个来源冲突，生成独立 claim 并说明差异。

#### 后端解析与降级

新增：

```text
StructuredAnswerParser
```

流程：

1. 尝试解析 LLM JSON。
2. 校验 `sourceIds` 是否存在于 EvidencePack。
3. 删除空 claim。
4. 如果解析失败，降级为当前纯文本回答，并设置 `claims=[]`。
5. 如果 claim 没有 sourceIds，标记为 `low`，前端展示风险提示。

新增日志字段：

```sql
ALTER TABLE rag_chat_log
ADD COLUMN claims_json TEXT,
ADD COLUMN structured_answer_valid BOOLEAN DEFAULT FALSE;
```

#### 接口兼容

当前 `ChatResponse` 保持：

```text
answer
noAnswer
sources
```

新增可选字段：

```text
claims
structuredAnswerValid
```

这样旧前端仍可运行，新前端可以逐步使用 claims。

#### 前端实现

ChatMessage 增强：

- 回答正文下展示 claims 列表。
- 每个 claim 后显示 source 标签。
- 点击 source 标签定位到 sources 区块。
- 没有 source 的 claim 用 warning 样式标记。

#### 验收标准

- 正常回答至少包含一条 claim。
- claim 的 sourceIds 都能对应到返回 sources。
- JSON 解析失败不影响用户看到答案。
- Chat log detail 可以查看原始 claims JSON。

### 4. 评测集和一键评测

#### 目标

建立 RAG 回归测试能力，支持对检索、拒答和回答依据做持续评估。第一版重点评测来源命中和拒答，不做复杂语义评分。

#### 数据模型

新增评测集：

```sql
CREATE TABLE rag_eval_set (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    collection_ids TEXT,
    document_ids TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);
```

新增样例：

```sql
CREATE TABLE rag_eval_case (
    id BIGSERIAL PRIMARY KEY,
    eval_set_id BIGINT NOT NULL REFERENCES rag_eval_set(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    expected_no_answer BOOLEAN NOT NULL DEFAULT FALSE,
    expected_source_chunk_ids TEXT,
    expected_answer_keywords TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);
```

新增运行结果：

```sql
CREATE TABLE rag_eval_run (
    id BIGSERIAL PRIMARY KEY,
    eval_set_id BIGINT NOT NULL REFERENCES rag_eval_set(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    total_cases INT NOT NULL DEFAULT 0,
    passed_cases INT NOT NULL DEFAULT 0,
    recall_hit_rate DOUBLE PRECISION,
    no_answer_accuracy DOUBLE PRECISION,
    avg_latency_ms BIGINT,
    started_at TIMESTAMP DEFAULT now(),
    finished_at TIMESTAMP
);

CREATE TABLE rag_eval_run_case (
    id BIGSERIAL PRIMARY KEY,
    eval_run_id BIGINT NOT NULL REFERENCES rag_eval_run(id) ON DELETE CASCADE,
    eval_case_id BIGINT NOT NULL REFERENCES rag_eval_case(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer TEXT,
    no_answer BOOLEAN,
    retrieved_chunk_ids TEXT,
    source_hit BOOLEAN,
    no_answer_match BOOLEAN,
    keyword_match BOOLEAN,
    latency_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT now()
);
```

#### 指标定义

第一版指标：

- `source_hit`：返回 sources 中至少命中一个 `expected_source_chunk_ids`。
- `no_answer_match`：实际 noAnswer 与 expectedNoAnswer 一致。
- `keyword_match`：回答包含 expectedAnswerKeywords 中配置的关键词。
- `case_passed`：如果 expectedNoAnswer=true，则只看 noAnswerMatch；否则看 sourceHit 和 keywordMatch。

聚合指标：

- `recall_hit_rate = source_hit / 非拒答案例数`
- `no_answer_accuracy = no_answer_match / 全部样例数`
- `pass_rate = passed_cases / total_cases`

#### 接口设计

```text
GET  /api/rag/eval/sets
POST /api/rag/eval/sets
GET  /api/rag/eval/sets/{id}
POST /api/rag/eval/sets/{id}/cases
PATCH /api/rag/eval/cases/{id}
POST /api/rag/eval/sets/{id}/runs
GET  /api/rag/eval/runs/{id}
GET  /api/rag/eval/runs/{id}/cases
```

从检索调试结果创建样例：

```text
POST /api/rag/eval/cases/from-retrieval-debug
```

#### 后端实现

新增：

```text
EvalSetService
EvalCaseService
EvalRunService
EvalScorer
```

运行方式：

- 第一版同步运行小评测集。
- 超过阈值后接入任务持久化。

#### 前端实现

新增页面：

```text
/eval
/eval/sets/:id
/eval/runs/:id
```

展示：

- 样例列表。
- 一键运行。
- 本次命中率、拒答准确率、失败样例。
- 单个失败样例的 retrieval snapshot。

#### 验收标准

- 可以手动创建评测样例。
- 可以从检索调试结果创建评测样例。
- 可以运行评测集并持久化结果。
- 可以清楚看到每个失败样例失败在哪个指标。

### 5. 任务持久化

#### 目标

把当前内存中的索引进度升级为可持久化、可恢复、可查询的任务系统。第一版先覆盖 document index 和 embedding。

#### 任务状态机

```text
PENDING
  -> RUNNING
  -> SUCCEEDED
  -> FAILED
  -> CANCELED
```

补充状态：

```text
RETRYING
```

第一版可以不实现真正取消，但保留 `CANCELED` 状态。

#### 任务类型

```text
DOCUMENT_INDEX
DOCUMENT_EMBEDDING
KEYWORD_INDEX
EVAL_RUN
```

第一阶段实现：

- `DOCUMENT_INDEX`
- `DOCUMENT_EMBEDDING`

后续再接：

- `KEYWORD_INDEX`
- `EVAL_RUN`

#### 数据模型

```sql
CREATE TABLE rag_task (
    id BIGSERIAL PRIMARY KEY,
    task_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    stage VARCHAR(50),
    progress_percent INT NOT NULL DEFAULT 0,
    message TEXT,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 0,
    request_json TEXT,
    result_json TEXT,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_rag_task_target
ON rag_task(target_type, target_id, created_at DESC);

CREATE INDEX idx_rag_task_status
ON rag_task(status, created_at DESC);
```

#### 后端实现

新增：

```text
task/entity/RagTask.java
task/repository/RagTaskMapper.java
task/service/TaskService.java
task/service/TaskProgressReporter.java
task/controller/TaskController.java
```

改造：

- `DocumentIndexTaskService.submitIndex` 创建 `rag_task`。
- `DocumentIndexTaskService.submitEmbedding` 创建 `rag_task`。
- 当前 `ProgressSnapshot` 改为从 task 表读取。
- 任务执行过程中更新 task 的 stage、progress、message。

重启恢复策略：

- 应用启动时扫描 `RUNNING` 和 `RETRYING` 任务。
- 第一版保守处理：全部标记为 `FAILED`，errorMessage 为 `Service restarted before task completed`。
- 后续支持真正恢复时再加入幂等 task runner。

并发控制：

- 同一个 document 同一时间只允许一个 RUNNING/PENDING 任务。
- 使用数据库查询和唯一约束防重复提交。

#### 接口设计

```text
GET  /api/rag/tasks
GET  /api/rag/tasks/{id}
GET  /api/rag/documents/{id}/tasks/latest
POST /api/rag/tasks/{id}/retry
POST /api/rag/tasks/{id}/cancel
```

文档现有接口响应增加 `taskId`：

```json
{
  "documentId": 1,
  "taskId": 20,
  "status": "QUEUED",
  "message": "Document index task accepted"
}
```

#### 前端实现

- Documents 页面操作按钮提交后根据 taskId 轮询任务状态。
- Document Detail 页面展示最新任务。
- 新增任务历史列表。
- 失败任务支持 Retry。

#### 验收标准

- 服务重启后任务状态不会消失。
- 文档详情页可以看到最近任务状态。
- 同一文档重复点击不会创建多个并发任务。
- 任务失败时可看到明确错误信息。

### 6. 笔记、摘录、学习卡片

#### 目标

把问答和阅读过程中的有价值内容沉淀为个人知识资产。第一版只做结构化保存和基础列表，不做复杂双链和 spaced repetition。

#### 边界定义

```text
Note：用户主动写的笔记，可以引用问答、chunk 或摘录。
Excerpt：从 chunk 或回答中保存的一段原文摘录。
Highlight：原文中的高亮范围，通常绑定 chunk。
Card：面向复习的问答卡或概念卡。
```

第一版实现：

- Note。
- Excerpt。
- Card。

Highlight 需要原文定位能力更强，可以放第二版。

#### 数据模型

```sql
CREATE TABLE rag_note (
    id BIGSERIAL PRIMARY KEY,
    collection_id BIGINT REFERENCES rag_collection(id),
    title VARCHAR(300),
    content TEXT NOT NULL,
    source_type VARCHAR(50),
    source_id BIGINT,
    tags TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE rag_excerpt (
    id BIGSERIAL PRIMARY KEY,
    collection_id BIGINT REFERENCES rag_collection(id),
    document_id BIGINT REFERENCES rag_document(id) ON DELETE SET NULL,
    chunk_id BIGINT REFERENCES rag_document_chunk(id) ON DELETE SET NULL,
    chat_log_id BIGINT REFERENCES rag_chat_log(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    note TEXT,
    tags TEXT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE rag_study_card (
    id BIGSERIAL PRIMARY KEY,
    collection_id BIGINT REFERENCES rag_collection(id),
    card_type VARCHAR(50) NOT NULL,
    front TEXT NOT NULL,
    back TEXT NOT NULL,
    source_type VARCHAR(50),
    source_id BIGINT,
    source_chunk_id BIGINT,
    tags TEXT,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);
```

`source_type` 取值：

```text
CHAT_LOG
CHUNK
EXCERPT
MANUAL
```

#### 接口设计

Notes：

```text
GET  /api/rag/notes
POST /api/rag/notes
GET  /api/rag/notes/{id}
PATCH /api/rag/notes/{id}
DELETE /api/rag/notes/{id}
```

Excerpts：

```text
GET  /api/rag/excerpts
POST /api/rag/excerpts
PATCH /api/rag/excerpts/{id}
DELETE /api/rag/excerpts/{id}
```

Cards：

```text
GET  /api/rag/cards
POST /api/rag/cards
PATCH /api/rag/cards/{id}
POST /api/rag/cards/{id}/archive
```

从回答生成笔记：

```text
POST /api/rag/notes/from-chat-log/{chatLogId}
```

从 chunk 生成卡片：

```text
POST /api/rag/cards/from-chunk/{chunkId}
```

#### 后端实现

新增模块：

```text
note/
excerpt/
card/
```

新增生成服务：

```text
CardGenerationService
DocumentSummaryNoteService
```

第一版生成卡片时可以直接调用 LLM：

输入：

- chunk content。
- document title。
- chapter title。

输出：

- front。
- back。
- tags。

#### 前端实现

新增页面：

```text
/notes
/notes/:id
/excerpts
/cards
```

入口：

- Chat answer 下方提供“保存为笔记”。
- SourceList 中每个来源提供“保存摘录”。
- ChunkPreview 提供“生成卡片”。

#### 验收标准

- 可以从回答保存笔记。
- 可以从引用来源保存摘录。
- 可以从 chunk 生成学习卡片。
- Note、Excerpt、Card 都能按 Collection 和 tag 过滤。
- 删除笔记不会删除原始文档、chunk 或 chat log。
