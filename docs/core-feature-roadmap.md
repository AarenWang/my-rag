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

需要先明确：

- Collection 和 Document 的关系。
- 一个文档是否允许属于多个 Collection。
- 检索时按 Collection 过滤还是展开为 documentIds 过滤。
- Collection 的统计字段、标签、描述和归档策略。
- 未来接入权限时是否以 Collection 作为授权边界。

### 2. 多轮会话

需要先明确：

- conversation、message、retrieval snapshot 的表结构。
- 每轮追问如何结合历史上下文改写查询。
- 多轮会话是否继承上一轮文档范围。
- 每轮回答是否独立保存 evidence 和 sources。
- 历史消息进入 LLM prompt 的截断策略。

### 3. Claim-source 结构化回答

需要先明确：

- LLM 输出 JSON 结构。
- 结构化解析失败时如何降级。
- claim 和 sourceIds 的绑定粒度，是句子级、段落级还是要点级。
- 前端如何展示无引用 claim。
- 日志中是否保存完整 claims。

### 4. 评测集和一键评测

需要先明确：

- 评测样例字段。
- expectedAnswer 是严格文本、语义判断还是只校验来源。
- expectedSourceChunkIds 如何维护。
- 评测指标定义，例如 recall hit rate、source precision、no-answer accuracy。
- 评测运行结果是否持久化。

### 5. 任务持久化

需要先明确：

- 任务状态机。
- 任务类型，例如 index、embedding、reindex、keyword-index。
- 服务重启后如何恢复 RUNNING 状态。
- 是否支持取消、重试、批量任务。
- 现有内存进度和任务表的职责划分。

### 6. 笔记、摘录、学习卡片

需要先明确：

- note、highlight、excerpt、card 的边界。
- 是否都归属 Collection。
- 是否允许从问答、chunk、原文三种入口创建。
- 是否需要双向链接到 source chunk。
- 前端主工作流是阅读视图、问答视图还是独立知识整理视图。
