FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

COPY src src

RUN ./gradlew clean bootJar --no-daemon && \
    JAR_FILE="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" && \
    test -n "$JAR_FILE" && \
    cp "$JAR_FILE" build/libs/app.jar


FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd --system spring && \
    useradd --system --gid spring spring

COPY --from=builder --chown=spring:spring \
    /workspace/build/libs/app.jar \
    /app/app.jar

USER spring:spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
