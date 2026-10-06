@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
"C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd" clean compile -DskipTests
