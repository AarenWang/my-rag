# My RAG

中文电子书 RAG MVP，用于跑通文档上传、解析、chunk 切分、embedding、pgvector 检索、带来源引用的问答闭环。

## 当前能力

- 上传文档并持久化元数据。
- 文档索引分为两步：先 parse + chunk 到 `CHUNKED`，再由用户确认 embedding 费用后生成向量到 `READY`。
- embedding 费用预估：按 chunk 的本地 `tokenCount` 汇总，公式为 `estimatedTokens / 1000 * pricePer1kTokens`。
- 基于 pgvector 的相似度检索。
- Chat 问答：按选中文档召回上下文，调用 LLM 生成回答，并返回来源引用。
- Chat log：记录问题、回答、来源和无答案状态。
- 前端页面：Dashboard、Documents、Document Detail、Chat、Chat Logs、Settings。

> 费用预估只使用本地 token 估算值，最终费用以 DashScope 账单为准。

## 技术栈

Backend:

- Java 17
- Spring Boot 3
- Maven 多模块
- PostgreSQL + pgvector
- Flyway
- MyBatis Plus

Frontend:

- pnpm workspace
- Vite
- React
- TypeScript
- React Router
- TanStack Query
- Ant Design

## 配置约定

本项目把可提交配置和本地机密配置分开：

```text
backend/rag-service/src/main/resources/application-local.yml
```

这个文件会提交到 Git，只能放安全的本地默认值和环境变量占位符。不要写真实数据库密码、API Key、私有主机地址或个人路径。

```text
backend/rag-service/application-local-secret.yml
```

这个文件不会提交，是本机真正存放机密信息的位置。可以从示例文件复制：

```powershell
Copy-Item backend\rag-service\application-local-secret.example.yml backend\rag-service\application-local-secret.yml
```

也可以不用 secret YAML，直接通过环境变量传入，例如 `RAG_DATASOURCE_PASSWORD`、`RAG_EMBEDDING_API_KEY`、`RAG_CHAT_API_KEY`。

## 本地数据库

项目提供了一个本地 PostgreSQL + pgvector 容器：

```powershell
docker compose up -d postgres
```

如果使用外部 PostgreSQL，需要提前启用 pgvector：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

常用数据库环境变量：

```powershell
$env:RAG_DATASOURCE_URL='jdbc:postgresql://<host>:5432/<database>?sslmode=disable'
$env:RAG_DATASOURCE_USERNAME='<user>'
$env:RAG_DATASOURCE_PASSWORD='<password>'
```

## 模型配置

默认本地 profile 使用 mock provider，方便无 API Key 启动。接真实服务时建议把下面配置放入 `backend/rag-service/application-local-secret.yml`。

DashScope embedding:

```yaml
rag:
  model:
    embedding-provider: dashscope
    embedding-base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    embedding-api-key: <dashscope-api-key>
    embedding-model: text-embedding-v4
    embedding-dimension: 1024
    embedding-batch-size: 10
    embedding-price-per-1k-tokens: 0.0005
```

新加坡地域可把 `embedding-base-url` 改为：

```text
https://dashscope-intl.aliyuncs.com/compatible-mode/v1
```

OpenAI-compatible chat，例如 DeepSeek:

```yaml
rag:
  model:
    chat-provider: openai-compatible
    chat-base-url: https://api.deepseek.com
    chat-api-key: <deepseek-api-key>
    chat-model: deepseek-v4-flash
    chat-temperature: 0.2
```

对应环境变量：

```powershell
$env:RAG_EMBEDDING_PROVIDER='dashscope'
$env:RAG_EMBEDDING_BASE_URL='https://dashscope.aliyuncs.com/compatible-mode/v1'
$env:RAG_EMBEDDING_API_KEY='<dashscope-api-key>'
$env:RAG_EMBEDDING_MODEL='text-embedding-v4'
$env:RAG_EMBEDDING_DIMENSION='1024'
$env:RAG_EMBEDDING_BATCH_SIZE='10'
$env:RAG_EMBEDDING_PRICE_PER_1K_TOKENS='0.0005'

$env:RAG_CHAT_PROVIDER='openai-compatible'
$env:RAG_CHAT_BASE_URL='https://api.deepseek.com'
$env:RAG_CHAT_API_KEY='<deepseek-api-key>'
$env:RAG_CHAT_MODEL='deepseek-v4-flash'
$env:RAG_CHAT_TEMPERATURE='0.2'
```

## 启动 Backend

```powershell
cd backend
mvn -pl rag-service -am package -DskipTests
java -jar rag-service\target\rag-service-0.0.1-SNAPSHOT.jar --spring.config.additional-location=file:rag-service/application-local-secret.yml
```

也可以用 Spring Boot 插件启动本地 profile：

```powershell
cd backend
mvn -pl rag-service spring-boot:run -Dspring-boot.run.profiles=local
```

健康检查：

```text
GET http://localhost:8080/api/rag/health
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## 启动 Frontend

```powershell
cd frontend
pnpm install
pnpm dev:web
```

默认访问：

```text
http://localhost:3000
```

Chat 页面：

```text
http://localhost:3000/chat
```

## 主要 API

Documents:

- `GET /api/rag/documents`
- `POST /api/rag/documents/upload`
- `GET /api/rag/documents/{id}/status`
- `POST /api/rag/documents/{id}/index`
- `GET /api/rag/documents/{id}/embedding/estimate`
- `POST /api/rag/documents/{id}/embedding`
- `GET /api/rag/documents/{id}/index/progress`

Chunks:

- `GET /api/rag/chunks?documentId={id}`

Chat:

- `POST /api/rag/chat`
- `GET /api/rag/chat/logs`
- `GET /api/rag/chat/logs/{id}`

## 推荐使用流程

1. 启动 PostgreSQL。
2. 配置 `application-local-secret.yml` 或环境变量。
3. 启动 backend。
4. 启动 frontend。
5. 在 Documents 上传文档。
6. 点击 Re-index，让文档完成 parse + chunk，状态变为 `CHUNKED`。
7. 查看 embedding 费用预估并确认，完成后状态变为 `READY`。
8. 在 Chat 页面选择 READY 文档并提问。

## 验证命令

Backend:

```powershell
cd backend
mvn -pl rag-service -am test-compile
```

Frontend:

```powershell
cd frontend
pnpm --filter @my-rag/web build
```
