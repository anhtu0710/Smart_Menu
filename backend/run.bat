@echo off
setlocal
if "%GEMINI_API_KEY%"=="" (
  echo GEMINI_API_KEY must be set before starting the backend.
  exit /b 1
)
if "%DB_PASSWORD%"=="" (
  echo DB_PASSWORD must be set before starting the backend.
  exit /b 1
)
mvn spring-boot:run
