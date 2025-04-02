FROM ghcr.io/fintlabs/fint-drosjeloyve-admin AS node

FROM gradle:8.13-jdk21 AS builder
USER root
COPY . .
COPY --from=node /src/build/ src/main/resources/public/
RUN gradle --no-daemon clean build

FROM gcr.io/distroless/java21
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
WORKDIR /app
COPY --from=builder /home/gradle/src/main/resources/times.ttf /app/times.ttf
COPY --from=builder /home/gradle/build/libs/fint-drosjeloyve-service.jar /app/fint-drosjeloyve-service.jar
CMD ["/app/fint-drosjeloyve-service.jar"]
