# --- Build stage ---
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

COPY gradle              gradle
COPY gradlew             gradlew
COPY build.gradle.kts    build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts

COPY common-lib-module   common-lib-module
COPY user-auth-module    user-auth-module
COPY workflow-module     workflow-module
COPY ledger-module       ledger-module
COPY product-module      product-module
COPY app                 app

RUN chmod +x gradlew && ./gradlew :app:bootJar --no-daemon -x test

# --- Runtime stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
