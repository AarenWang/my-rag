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

