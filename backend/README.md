# My RAG Backend

Java/Spring Boot backend for the Chinese ebook RAG MVP.

## Modules

```text
rag-common   Common response and error classes
rag-api      API DTO contracts
rag-service  Executable Spring Boot application
```

## Local Database

From the repository root:

```powershell
docker compose up -d postgres
```

## Local Configuration

Configuration is split into two files:

```text
rag-service/src/main/resources/application-local.yml
```

This file is committed. Keep only safe local defaults and environment-variable
placeholders here. Do not put real database passwords, API keys, private hosts,
or personal paths in this file.

```text
rag-service/application-local-secret.yml
```

This file is ignored by Git and is the place for machine-specific secrets. Start
from `rag-service/application-local-secret.example.yml`:

```powershell
Copy-Item rag-service\application-local-secret.example.yml rag-service\application-local-secret.yml
```

Then fill in real values locally. Equivalent environment variables such as
`RAG_DATASOURCE_PASSWORD` and `RAG_EMBEDDING_API_KEY` are also safe to use
instead of the secret YAML file.

## Build

```powershell
cd backend
mvn clean package
```

## Run

```powershell
cd backend
mvn -pl rag-service spring-boot:run -Dspring-boot.run.profiles=local
```

Health endpoint:

```text
GET http://localhost:8080/api/rag/health
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```
