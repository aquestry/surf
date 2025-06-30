FROM gradle:jdk21 AS builder
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN --mount=type=cache,target=/home/gradle/.gradle gradle --no-daemon dependencies
COPY src ./src
RUN --mount=type=cache,target=/home/gradle/.gradle gradle --no-daemon shadowJar --parallel --build-cache -x test

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*-all.jar app.jar
EXPOSE 8080
CMD ["java","-jar","app.jar"]