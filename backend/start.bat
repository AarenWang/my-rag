@echo off
setlocal

cd /d "%~dp0"

call mvn -pl rag-service spring-boot:run -Dspring-boot.run.arguments="--spring.config.additional-location=file:rag-service/application-local-secret.yml"

endlocal
