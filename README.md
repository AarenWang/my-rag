# My RAG

中文电子书 RAG MVP，用于跑通文档解析、chunk 切分、embedding、pgvector 检索和基于来源引用的问答闭环。

## Backend

后端位于 `backend/`，采用 Java 17、Spring Boot 3、Maven 多模块、PostgreSQL + pgvector、Flyway、MyBatis Plus。

### 外部 PostgreSQL 配置

本项目默认使用外部 PostgreSQL 服务。数据库需要提前满足：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

本地推荐通过环境变量传入连接信息：

```powershell
$env:RAG_DATASOURCE_URL='jdbc:postgresql://<host>:5432/<database>?sslmode=disable'
$env:RAG_DATASOURCE_USERNAME='<user>'
$env:RAG_DATASOURCE_PASSWORD='<password>'
```

也可以创建本地私密配置文件：

```text
backend/rag-service/application-local-secret.yml
```

该文件已被 `.gitignore` 排除，不要提交数据库密码。

### 启动后端

```powershell
cd backend
mvn -pl rag-service -am package -DskipTests
java -jar rag-service/target/rag-service-0.0.1-SNAPSHOT.jar --spring.config.additional-location=file:rag-service/application-local-secret.yml
```

健康检查：

```text
GET http://localhost:8080/api/rag/health
```

Swagger UI：

```text
http://localhost:8080/swagger-ui.html
```

### DashScope embedding

The backend embedding client supports DashScope through its OpenAI-compatible API.
Use the following local environment variables, or put equivalent values in
`backend/rag-service/application-local-secret.yml`.

```powershell
$env:RAG_EMBEDDING_PROVIDER='dashscope'
$env:RAG_EMBEDDING_BASE_URL='https://dashscope.aliyuncs.com/compatible-mode/v1'
$env:RAG_EMBEDDING_API_KEY='<your-dashscope-api-key>'
$env:RAG_EMBEDDING_MODEL='text-embedding-v4'
$env:RAG_EMBEDDING_DIMENSION='1024'
$env:RAG_EMBEDDING_BATCH_SIZE='10'
```

For Singapore region, set:

```powershell
$env:RAG_EMBEDDING_BASE_URL='https://dashscope-intl.aliyuncs.com/compatible-mode/v1'
```

## Frontend

前端位于 `frontend/`，采用 pnpm workspace、Vite、React、TypeScript、React Router、TanStack Query、Ant Design。

```powershell
cd frontend
pnpm install
pnpm dev:web
```
