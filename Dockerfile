FROM maven:3-eclipse-temurin-21-alpine AS bob-the-builder
LABEL authors="kalyanmudumby"
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:resolve && mvn dependency:resolve-plugins
COPY . .
ARG ENV
RUN if [ -z "$ENV" ]; then mvn install -Pdefault; else mvn install -P"$ENV"; fi
FROM eclipse-temurin:21-alpine
RUN wget -O otel.jar  https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.9.0/opentelemetry-javaagent.jar
# CONFIGURABLE
ENV SERVICE_NAME="OPTIMUS"
ENV CLIENT_NAME="EARTH"
ENV INGESTOR_ENDPONT="https://<OPENTELEMETRY-COLLECTOR-HTTP-ENDPOINT>"
# DO NOT EDIT
ENV OTEL_EXPORTER_OTLP_ENDPOINT=$INGESTOR_ENDPONT
ARG ENV="development"
RUN echo $ENV
ENV OTEL_RESOURCE_ATTRIBUTES="service.name=$SERVICE_NAME-${ENV},environment=${ENV},client=$CLIENT_NAME"
ENV OTEL_TRACES_SAMPLER="always_on"
ENV OTEL_INSTRUMENTATION_MICROMETER_ENABLED=true
ENV OTEL_INSTRUMENTATION_COMMON_DB_STATEMENT_SANITIZER_ENABLED=true
ENV OTEL_INSTRUMENTATION_LOGBACK_ENABLED=true
ENV OTEL_METRIC_EXPORT_INTERVAL=10000
ENV OTEL_METRICS_EXEMPLAR_FILTER=ALWAYS_ON
ENV JAVA_OPTS="-javaagent:otel.jar"
COPY --from=bob-the-builder /build/target/documan*.jar application.jar
ENTRYPOINT ["java","-javaagent:otel.jar","-jar","application.jar"]
