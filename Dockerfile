FROM fintlabsacr.azurecr.io/drosjeloyve-frontend as node

FROM gradle:6.7.0-jdk8 as builder
USER root
COPY . .
COPY --from=node /src/build/ src/main/resources/public/
RUN gradle --no-daemon clean build

FROM gcr.io/distroless/java:8
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/src/main/resources/times.ttf /data/times.ttf
COPY --from=builder /home/gradle/build/libs/fint-drosjeloyve-service-*.jar /data/fint-drosjeloyve-service.jar
CMD ["/data/fint-drosjeloyve-service.jar"]
