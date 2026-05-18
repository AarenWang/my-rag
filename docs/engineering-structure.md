# 工程目录结构与前后端技术栈规划

## 1. 参考原则

本项目可以参考 `D:\workspace\crypto-pay` 的整体工程组织方式，但不直接照搬它的服务拆分复杂度。

可参考的部分：

1. 仓库顶层按 `backend`、`frontend`、`docs`、`scripts`、`e2e` 分区。
2. 后端使用 Java / Spring Boot / Maven，保持清晰的模块边界。
3. 前端使用 pnpm workspace，将应用、API client、类型和 UI 组件分开。
4. 保留脚本、文档和端到端测试目录，方便后续工程化。

不建议照搬的部分：

1. 不在 MVP 阶段拆成多个后端微服务。
2. 不引入过重的网关、注册中心、配置中心。
3. 不参考 crypto-pay 的数据库选型，本项目固定使用 PostgreSQL + pgvector。
4. 不在第一阶段做复杂权限、商户、运营后台等业务分层。

## 2. 推荐仓库结构

```text
my-rag/
  backend/
    pom.xml
    rag-api/
    rag-service/
    rag-common/
  frontend/
    package.json
    pnpm-workspace.yaml
    apps/
      web/
    packages/
      api/
      types/
      ui/
      utils/
  docs/
    mvp.md
    engineering-structure.md
    api.md
    database.md
  scripts/
    dev/
    db/
  e2e/
  docker-compose.yml
  README.md
```

## 3. 后端技术栈

### 3.1 基础栈

```text
语言：Java 17
框架：Spring Boot 3.x
构建：Maven
数据库：PostgreSQL
向量检索：pgvector
数据库迁移：Flyway
ORM：MyBatis Plus 或 Spring Data JDBC
接口文档：springdoc-openapi / Swagger UI
HTTP 客户端：OkHttp 或 Spring WebClient
文档解析：Apache Tika、PDFBox、EPUB parser
测试：JUnit 5、Testcontainers
```

### 3.2 ORM 选择建议

MVP 推荐使用 `MyBatis Plus`，原因是：

1. 和 Java 后端常见业务项目更接近。
2. SQL 可控，便于写 pgvector 查询。
3. 对后续复杂检索 SQL、过滤条件、分页查询更友好。

如果希望更轻量，也可以使用 `Spring Data JDBC`，但 pgvector 自定义 SQL 场景下 MyBatis 更直接。

### 3.3 后端模块划分

MVP 后端建议做 Maven 多模块，但只启动一个 Spring Boot 应用。

```text
backend/
  pom.xml
  rag-api/
  rag-service/
  rag-common/
```

### 3.4 模块职责

```text
rag-api
  对外 REST API、Controller、请求响应 DTO、OpenAPI 注解。

rag-service
  核心业务实现，包括文档处理、切片、embedding、检索、问答、任务状态流转。

rag-common
  通用模型、异常、工具类、统一响应、配置常量。
```

第一阶段不要拆 `document-service`、`embedding-service`、`chat-service` 这种独立进程。可以在 `rag-service` 内部按 package 拆领域模块，后面需要扩展时再拆服务。

## 4. 后端包结构

推荐包名：

```text
com.my.rag
```

`rag-service` 内部结构：

```text
rag-service/
  src/main/java/com/my/rag/
    RagApplication.java
    config/
      RagProperties.java
      OpenApiConfig.java
      WebConfig.java
    document/
      controller/
      service/
      repository/
      entity/
      dto/
      enums/
    chunk/
      service/
      repository/
      entity/
      dto/
    embedding/
      service/
      client/
      repository/
      entity/
      dto/
    retrieval/
      service/
      dto/
    chat/
      controller/
      service/
      repository/
      entity/
      dto/
    parser/
      service/
      strategy/
    common/
      error/
      response/
      util/
  src/main/resources/
    application.yml
    application-local.yml
    db/migration/
      V1__init_pgvector.sql
      V2__create_rag_tables.sql
```

### 4.1 领域模块说明

```text
document
  文件上传、文件 hash、文档状态机、文档列表。

parser
  TXT / Markdown / EPUB / PDF 文本解析策略。

chunk
  章节识别、自然段合并、overlap、chunk metadata。

embedding
  embedding 模型适配、批量向量化、向量入库。

retrieval
  问题向量化、pgvector topK、scoreThreshold 过滤。

chat
  prompt 组装、LLM 调用、answer + sources 返回、问答日志。
```

## 5. 前端技术栈

### 5.1 基础栈

```text
包管理：pnpm
构建：Vite
框架：React + TypeScript
路由：React Router
请求：fetch 封装或 axios
服务端状态：TanStack Query
组件库：Ant Design
样式：Tailwind CSS，可选
状态管理：Zustand，可选
测试：Playwright，第二阶段加入
```

MVP 前端不需要过度设计，核心是让 RAG 链路可视化、可调试。

### 5.2 前端目录

参考 crypto-pay 的 `apps + packages` 方式，但只保留一个应用：

```text
frontend/
  package.json
  pnpm-workspace.yaml
  turbo.json
  apps/
    web/
      package.json
      vite.config.ts
      index.html
      src/
        main.tsx
        App.tsx
        router.tsx
        layouts/
          MainLayout.tsx
        pages/
          Dashboard.tsx
          Documents.tsx
          DocumentDetail.tsx
          Chat.tsx
          Settings.tsx
        components/
          UploadPanel.tsx
          DocumentStatusBadge.tsx
          ChunkPreview.tsx
          SourceList.tsx
          ChatMessage.tsx
        stores/
        hooks/
  packages/
    api/
      src/
        client.ts
        documents.ts
        chat.ts
        index.ts
    types/
      src/
        document.ts
        chat.ts
        index.ts
    ui/
      src/
        index.ts
    utils/
      src/
        index.ts
```

### 5.3 页面规划

MVP 只需要 4 个页面：

```text
Dashboard
  展示文档数量、READY 数量、最近问答、处理失败数。

Documents
  上传文档、查看文档列表、触发 index、查看状态。

DocumentDetail
  查看文档 metadata、chunk 列表、chunk 内容预览。

Chat
  选择文档、输入问题、展示回答、展示引用来源。
```

`Settings` 可以先放配置展示，不急着做可编辑。

## 6. 顶层工程文件

建议顶层保留这些文件：

```text
README.md
  项目介绍、快速启动、技术栈。

docker-compose.yml
  启动 PostgreSQL + pgvector。

.gitignore
  Java、Node、IDE、上传文件目录等忽略规则。

docs/
  产品和技术文档。

scripts/
  本地开发、数据库初始化、测试脚本。
```

## 7. docker-compose 规划

MVP 至少需要 PostgreSQL + pgvector：

```text
services:
  postgres:
    image: pgvector/pgvector:pg16
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: my_rag
      POSTGRES_USER: rag
      POSTGRES_PASSWORD: rag
```

后续可以加入：

```text
minio
  存储原始文档文件。

ollama
  本地 embedding 或本地 LLM 调试。

redis
  异步任务队列和缓存。
```

MVP 第一阶段可以先不加 Redis，用 Spring `@Async` 或同步接口跑通。

## 8. 技术选型结论

### 8.1 后端结论

```text
Java 17
Spring Boot 3.x
Maven 多模块
PostgreSQL + pgvector
Flyway
MyBatis Plus
springdoc-openapi
Apache Tika / PDFBox / EPUB parser
OkHttp 或 WebClient
JUnit 5 + Testcontainers
```

### 8.2 前端结论

```text
pnpm workspace
Vite
React
TypeScript
React Router
TanStack Query
Ant Design
Tailwind CSS 可选
Playwright 第二阶段加入
```

### 8.3 工程形态结论

```text
后端：Maven 多模块，单 Spring Boot 应用。
前端：pnpm workspace，单 web app + 可复用 packages。
数据库：PostgreSQL + pgvector。
部署：本地 docker-compose 启动数据库，前后端本地开发。
```

## 9. 初始化顺序

建议按以下顺序落地：

1. 创建顶层目录：`backend`、`frontend`、`docs`、`scripts`、`e2e`。
2. 创建 `docker-compose.yml`，先跑起 PostgreSQL + pgvector。
3. 初始化后端 Maven 多模块：`rag-common`、`rag-api`、`rag-service`。
4. 接入 Flyway，创建 pgvector 扩展和 RAG 核心表。
5. 初始化前端 pnpm workspace 和 `apps/web`。
6. 先打通文档列表和上传接口。
7. 再实现文档解析、chunk、embedding、chat。
8. 最后补前端 Chat 页面和引用来源展示。

## 10. 暂缓决策

以下问题可以先不定死，等 MVP 跑通后根据实际效果调整：

1. embedding 模型最终选云端还是本地。
2. 是否引入 reranker。
3. 是否引入 Redis 做异步任务队列。
4. 是否把文件存储从本地磁盘迁移到 MinIO。
5. 是否把后端拆成多个服务。
6. 是否引入完整的权限系统。

先把单体工程做扎实，再拆服务会更稳。
