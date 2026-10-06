FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd --system --create-home --uid 10001 smartmenu \
    && mkdir -p /app/uploads \
    && chown -R smartmenu:smartmenu /app

COPY --from=build /workspace/target/smartmenu-backend-1.0.0.jar /app/app.jar

USER smartmenu
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
