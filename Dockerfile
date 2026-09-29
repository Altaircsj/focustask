# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
# Database integration tests run separately with Docker available, not inside image build.
RUN mvn -B -ntp -DskipTests package && cp target/focustask-*.jar /build/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN groupadd --system focustask && useradd --system --gid focustask --home-dir /app focustask
COPY --from=build --chown=focustask:focustask /build/app.jar /app/app.jar
USER focustask
# This bounds heap, not total JVM memory; leave room for metaspace, threads and native memory.
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx300m -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
