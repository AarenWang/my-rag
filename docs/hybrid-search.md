# Hybrid Search 技术方案

## 目标

RAG 检索不能只依赖向量召回。向量召回负责“语义相关”，关键词召回负责“字面证据”，两者融合后再交给 LLM，整体会更稳。

目标流程：

```text
用户问题
  -> 向量召回 Vector Recall
  -> 关键词召回 Keyword / BM25 Recall
  -> 结果合并去重
  -> RRF 融合排序
  -> Reranker 重排
  -> Context Builder 组装证据包
  -> LLM grounded answer
```

第一版实现允许没有真实 reranker，但代码结构必须预留：

```text
RRF -> NoopReranker -> ContextBuilder
```

后续接入 `bge-reranker-v2-m3`、`gte-reranker` 等模型时，只替换 reranker client。

## 当前实施状态

- [x] 采用方案 B：单独创建 `rag_chunk_search_index` 表。
- [x] 新增 keyword search mapper/service。
- [x] 新增 `vector` / `hybrid` 检索模式配置。
- [x] 新增 RRF 融合排序。
- [x] 默认关闭 keyword index，避免未安装 `pg_jieba` / `zhparser` 时影响现有链路。
- [ ] 新增结构化 candidate / evidence pack。
- [ ] 新增 NoopReranker。
- [ ] 新增 ContextBuilder，负责去重、截断、引用编号。
- [ ] 安装 `pg_jieba` 或 `zhparser` 后开启并验收。

## 为什么不用 LIKE

不采用：

```sql
content LIKE '%keyword%'
```

原因：

- 无法利用中文分词索引。
- 数据量上来后性能差。
- 对中文词边界、术语、章节标题的处理不稳定。

正式方案使用 PostgreSQL full-text search：

```text
pg_jieba / zhparser + tsvector + GIN index
```

## 数据库方案 B

单独建全文索引表：

```sql
CREATE TABLE rag_chunk_search_index (
    id BIGSERIAL PRIMARY KEY,
    chunk_id BIGINT NOT NULL REFERENCES rag_document_chunk(id) ON DELETE CASCADE,
    document_id BIGINT NOT NULL REFERENCES rag_document(id) ON DELETE CASCADE,
    search_text TEXT NOT NULL,
    search_vector tsvector NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    UNIQUE (chunk_id)
);

CREATE INDEX idx_rag_chunk_search_index_vector
ON rag_chunk_search_index
USING gin(search_vector);

CREATE INDEX idx_rag_chunk_search_index_document_id
ON rag_chunk_search_index(document_id);
```

优点：

- chunk 原文表和全文索引解耦。
- 后续可以记录分词配置、索引版本、索引状态。
- 重建 keyword index 时不影响 chunk 原始数据。

注意：

- 当前先不在 Flyway 中安装 `pg_jieba` / `zhparser`。
- `keyword-index-enabled=false` 时，chunk 写入不会生成 search vector。

## 配置建议

当前默认保持 vector-only：

```yaml
rag:
  retrieval:
    mode: vector
    keyword-index-enabled: false
    text-search-config: jiebacfg
```

安装中文分词扩展并重建索引后，再开启 hybrid：

```yaml
rag:
  retrieval:
    mode: hybrid
    vector-top-k: 30
    keyword-top-k: 30
    rrf-top-k: 30
    rerank-top-k: 8
    context-top-k: 6
    score-threshold: 0.35
    rrf-k: 60
    text-search-config: jiebacfg
    keyword-index-enabled: true
    max-context-chars: 8000
```

参数解释：

- `vector-top-k`：向量召回候选数量。
- `keyword-top-k`：关键词召回候选数量。
- `rrf-top-k`：RRF 融合后保留的候选数量。
- `rerank-top-k`：reranker 后保留的候选数量。
- `context-top-k`：最终提交给 LLM 的 chunk 数量。
- `max-context-chars`：最终上下文最大字符预算。
- `rrf-k`：RRF 平滑参数，默认 60。

## Keyword Search SQL

`pg_jieba` 示例：

```sql
SELECT
    d.id AS document_id,
    d.title AS document_title,
    c.chapter_title,
    c.id AS chunk_id,
    c.chunk_index,
    c.start_paragraph,
    c.end_paragraph,
    c.content,
    ts_rank_cd(s.search_vector, plainto_tsquery('jiebacfg', :question)) AS keyword_score
FROM rag_chunk_search_index s
JOIN rag_document_chunk c ON c.id = s.chunk_id
JOIN rag_document d ON d.id = c.document_id
WHERE d.status = 'READY'
  AND s.search_vector @@ plainto_tsquery('jiebacfg', :question)
ORDER BY keyword_score DESC
LIMIT :topK;
```

`zhparser` 时把 `jiebacfg` 换成 `zhcfg`。

## RRF 融合排序

向量分数和 BM25 / `ts_rank_cd` 分数不是同一量纲，第一版不直接相加。

RRF 公式：

```text
rrfScore = 1 / (k + vectorRank) + 1 / (k + keywordRank)
```

如果某个 chunk 只被一路召回，则只计算这一路。

示例：

```text
chunk A: vectorRank=2, keywordRank=1
rrfScore = 1/(60+2) + 1/(60+1)

chunk B: vectorRank=1, keywordRank=null
rrfScore = 1/(60+1)
```

同时被两路高排名命中的 chunk 会自然排前。

## Reranker

Reranker 负责判断：

```text
question + chunk 是否真的能回答问题
```

第一版先实现接口和 Noop 版本：

```text
RerankerClient
  rerank(question, candidates, topK)

NoopRerankerClient
  直接按 RRF 顺序返回 topK
```

后续真实模型：

```text
bge-reranker-v2-m3
bge-reranker-large
gte-reranker
```

## Context Builder

最终提交给 LLM 的不是原始检索列表，而是 Evidence Pack。

Context Builder 职责：

- 按 `contextTopK` 控制 chunk 数量。
- 按 `maxContextChars` 控制总上下文长度。
- 按 `chunkId` 去重。
- 对同一文档、同一章节、相邻 chunk 做限量，避免上下文重复。
- 给每个证据分配稳定引用编号：`source_1`、`source_2`。
- 保留检索元数据，便于调试。

证据包示例：

```json
{
  "question": "Memory 和 Context Engineering 有什么区别？",
  "evidences": [
    {
      "sourceId": "source_1",
      "documentTitle": "Agent 系统设计",
      "chapterTitle": "第二章 上下文工程",
      "chunkId": 101,
      "content": "上下文工程关注……",
      "retrieval": {
        "vectorRank": 1,
        "keywordRank": 3,
        "vectorScore": 0.87,
        "keywordScore": 12.8,
        "rrfScore": 0.0323,
        "rerankScore": null,
        "retrievalSources": ["vector", "keyword"]
      }
    }
  ]
}
```

给 LLM 的文本格式：

```text
[source_1]
书名：Agent 系统设计
章节：第二章 上下文工程
chunkId：101
召回方式：vector, keyword
finalScore：0.0323
内容：
...
```

## 服务结构

建议落地结构：

```text
RetrievalService
  retrieve(query)
    -> vectorSearch
    -> keywordSearch
    -> HybridCandidate merge
    -> RRF
    -> RerankerClient
    -> return ranked chunks

RerankerClient
  NoopRerankerClient
  后续真实模型实现

ContextBuilder
  build(question, rankedChunks)
    -> EvidencePack

ChatService
  RetrievalService.retrieve
  ContextBuilder.build
  prompt + LLM
```

## 验收标准

开启 hybrid 后至少验证：

- 专有名词问题能被 keyword recall 命中。
- 配置项、代码标识符、环境变量能被召回。
- 章节标题类问题能被召回。
- 语义类问题仍能被 vector recall 稳定召回。
- RRF 后同时被两路召回的 chunk 排名更靠前。
- NoopReranker 不改变 RRF 顺序。
- Context Builder 能控制最终 chunk 数和总字符数。
- Chat 返回 sources 与提交给 LLM 的 `source_n` 一致。

## 实施顺序

1. 保留当前方案 B 表结构。
2. 扩展内部 retrieval DTO，记录 vector/keyword/rank/RRF/rerank 等信息。
3. 抽出 `HybridCandidate`。
4. 新增 `RerankerClient` 与 `NoopRerankerClient`。
5. 新增 `ContextBuilder` 和 Evidence Pack。
6. `ChatService` 改为消费 Evidence Pack。
7. 安装 `pg_jieba` / `zhparser` 后开启 `keyword-index-enabled` 并重建索引。
8. 用测试问题验收召回质量。
