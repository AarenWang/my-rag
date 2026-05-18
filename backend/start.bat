@echo off
setlocal

cd /d "%~dp0"

call mvn -pl rag-service -am package -DskipTests
if errorlevel 1 exit /b %errorlevel%

java -jar rag-service\target\rag-service-0.0.1-SNAPSHOT.jar --spring.config.additional-location=file:rag-service/application-local-secret.yml

endlocal
