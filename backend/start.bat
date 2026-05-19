@echo off
setlocal

cd /d "%~dp0"

set "RAG_UPLOAD_DIR=D:\data\my-rag\uploads"

if not exist "%RAG_UPLOAD_DIR%" mkdir "%RAG_UPLOAD_DIR%"

call mvn -pl rag-service -am package -DskipTests
if errorlevel 1 exit /b %errorlevel%

java -jar rag-service\target\rag-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --spring.config.additional-location=file:rag-service/application-local-secret.yml

endlocal
