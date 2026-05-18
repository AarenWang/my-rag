# Keyword Query Generation 方案

## 目标

Hybrid Search 中的关键词召回不能简单把用户原问题直接丢给全文检索。

当前简单做法：

```text
question -> plainto_tsquery(textSearchConfig, question)
```

问题是：

- 用户问题里有很多口语化停用词。
- 中文分词器未必能正确保留专有名词。
- 配置项、代码标识符、英文术语、数字条件不能被拆坏。
- 比较型问题通常需要覆盖多个概念。
- 单一 query 容易漏召回。

因此需要一个轻量的 `KeywordQueryService.generate(question)`，把用户问题转换为 1 到 3 条更适合全文检索的 keyword query。

## 放在 RAG 流程中的位置

```text
用户问题
  -> embedding
  -> vector recall

用户问题
  -> KeywordQueryService.generate(question)
  -> keyword recall

vector + keyword
  -> RRF
  -> reranker
  -> context builder
  -> LLM
```

## 第一版设计原则

第一版不引入 LLM query rewrite，只做规则生成。

原因：

- 成本低。
- 延迟低。
- 可解释。
- 便于测试。
- 不依赖额外模型。

第一版生成的 query 数量控制在：

```text
1 <= queryCount <= 3
```

避免关键词召回过度发散。

## 输入输出

输入：

```java
String question
```

输出：

```java
List<String> keywordQueries
```

示例：

```text
问题：
上下文工程和记忆系统有什么区别？

输出：
[
  "上下文工程 记忆系统 区别",
  "上下文 工程 记忆 系统",
  "区别"
]
```

示例：

```text
问题：
RAG_EMBEDDING_PRICE_PER_1K_TOKENS 在哪里配置？

输出：
[
  "RAG_EMBEDDING_PRICE_PER_1K_TOKENS 配置",
  "RAG_EMBEDDING_PRICE_PER_1K_TOKENS"
]
```

示例：

```text
问题：
第七章讲了哪些和 Memory 相关的内容？

输出：
[
  "第七章 Memory",
  "Memory",
  "第七章"
]
```

## 需要保留的强信号

这些 token 不应该被拆坏：

### 1. 配置项 / 环境变量

```text
RAG_EMBEDDING_PRICE_PER_1K_TOKENS
RAG_CHAT_MODEL
OPENAI_API_KEY
```

匹配规则：

```text
[A-Z][A-Z0-9_]{2,}
```

### 2. 代码标识符

```text
chunkId
KeywordRetrievalService
textSearchConfig
application-local.yml
```

匹配规则：

```text
[A-Za-z][A-Za-z0-9_.$-]{2,}
```

### 3. 模型名 / 产品名

```text
text-embedding-v4
bge-reranker-v2-m3
DeepSeek-V3
pg_jieba
zhparser
```

这类通常会被上面的英文/连字符规则覆盖。

### 4. 章节 / 数字条件

```text
第七章
第 7 章
7 天
30 分钟
1024 维
0.0005 元
```

匹配规则：

```text
第\\s*[一二三四五六七八九十百千万0-9]+\\s*[章节篇]
\\d+(\\.\\d+)?\\s*(天|小时|分钟|元|维|token|tokens|章|节)
```

### 5. 中英文混合术语

```text
Context Engineering
Memory 系统
Agent 记忆
Hybrid Search
```

英文连续词可以保留原顺序，也可以拆单词。

## 中文关键词提取

在未接入专门中文分词库前，第一版用轻量规则：

1. 去标点。
2. 去常见停用词。
3. 保留连续中文短语。
4. 对过长中文串做 2 到 6 字滑动片段补充。

### 停用词示例

```text
的
了
和
与
或
在
是
有哪些
是什么
什么意思
如何
怎么
哪里
哪个
请问
一下
相关
内容
区别
对比
说明
解释
```

注意：`区别`、`对比`、`说明`、`解释` 是否删除要看策略。

建议：

- 用于主查询时可以保留 `区别` / `对比`。
- 用于核心实体查询时可以删除它们。

## Query 生成策略

### 1. exactQuery

用于保留强信号：

```text
强信号 token + 少量意图词
```

例：

```text
RAG_EMBEDDING_PRICE_PER_1K_TOKENS 配置
第七章 Memory
```

### 2. entityQuery

用于实体和核心概念：

```text
中文核心词 + 英文术语 + 数字章节
```

例：

```text
上下文工程 记忆系统
Context Engineering Memory
```

### 3. broadQuery

用于兜底召回：

```text
去停用词后的问题压缩版
```

例：

```text
上下文 工程 记忆 系统 区别
```

最终最多取 3 条：

```text
exactQuery
entityQuery
broadQuery
```

去重后如果为空，退回原问题。

## 伪代码

```java
public List<String> generate(String question) {
    String normalized = normalize(question);

    List<String> strongTokens = extractStrongTokens(normalized);
    List<String> chapterTokens = extractChapterTokens(normalized);
    List<String> chineseTerms = extractChineseTerms(normalized);
    List<String> englishTerms = extractEnglishTerms(normalized);
    List<String> intentTerms = extractIntentTerms(normalized);

    List<String> queries = new ArrayList<>();

    String exactQuery = join(strongTokens, chapterTokens, intentTerms);
    if (hasText(exactQuery)) {
        queries.add(exactQuery);
    }

    String entityQuery = join(chineseTerms, englishTerms, chapterTokens);
    if (hasText(entityQuery)) {
        queries.add(entityQuery);
    }

    String broadQuery = compactWithoutStopWords(normalized);
    if (hasText(broadQuery)) {
        queries.add(broadQuery);
    }

    if (queries.isEmpty()) {
        queries.add(normalized);
    }

    return queries.stream()
        .map(this::normalizeSpaces)
        .filter(this::hasText)
        .distinct()
        .limit(3)
        .toList();
}
```

## KeywordRetrievalService 整合方式

当前：

```java
keywordRetrievalService.search(question, documentIds, topK)
```

建议改为：

```java
List<String> keywordQueries = keywordQueryService.generate(question);
keywordRetrievalService.search(keywordQueries, documentIds, topK);
```

内部流程：

```text
for each keywordQuery:
  fulltext search topK
  collect hits

merge by chunkId:
  keep best keywordScore
  keep best keywordRank
  record matchedQueries

sort by best keyword rank / score
limit keywordTopK
```

这样一个问题可以用多条关键词 query 召回，但最终仍然只返回去重后的 keyword hits。

## SQL 查询模式

第一版建议支持配置：

```yaml
rag:
  retrieval:
    keyword-query-mode: websearch
```

可选：

```text
plain     -> plainto_tsquery
websearch -> websearch_to_tsquery
phrase    -> phraseto_tsquery
```

建议默认：

```text
websearch
```

原因：

- 更接近用户自然语言。
- 支持引号、减号等 web search 语义。
- 比 `plainto_tsquery` 对查询文本更友好。

Mapper 可以分 3 个 SQL 方法，避免把函数名做字符串拼接。

## 示例

### 示例 1：概念比较

输入：

```text
Memory 和 Context Engineering 有什么区别？
```

输出：

```text
Memory Context Engineering 区别
Memory Context Engineering
Memory Context Engineering 区别
```

去重后：

```text
Memory Context Engineering 区别
Memory Context Engineering
```

### 示例 2：配置项

输入：

```text
RAG_EMBEDDING_PRICE_PER_1K_TOKENS 是在哪里设置的？
```

输出：

```text
RAG_EMBEDDING_PRICE_PER_1K_TOKENS 设置
RAG_EMBEDDING_PRICE_PER_1K_TOKENS
```

### 示例 3：章节问题

输入：

```text
第七章主要讲了什么？
```

输出：

```text
第七章
第七章 主要
```

### 示例 4：数字规则

输入：

```text
签收 7 天后还能退货吗？
```

输出：

```text
7 天 退货
签收 7 天 退货
```

## 验收标准

至少准备以下测试：

- 配置项：`RAG_EMBEDDING_PRICE_PER_1K_TOKENS 在哪里配置？`
- 模型名：`text-embedding-v4 的价格是多少？`
- 章节：`第七章主要讲什么？`
- 英文术语：`Context Engineering 是什么？`
- 中英混合：`Memory 和上下文有什么区别？`
- 数字条件：`1024 维 embedding 在哪里提到？`
- 无依据问题：确保不会因为 broadQuery 过宽而召回明显无关内容。

## 第一版实现边界

第一版做：

- 规则抽取。
- 生成最多 3 条 query。
- 保留强信号 token。
- 多 query keyword recall。
- 合并 keyword hits。

第一版不做：

- LLM query rewrite。
- 同义词扩展。
- 拼音纠错。
- 复杂实体识别。
- 用户词典管理。

这些可以等 hybrid search 跑通后再加。
