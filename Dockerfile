# syntax=docker/dockerfile:1

# ---------------------------------------------------------------- build
FROM maven:3-eclipse-temurin-25-alpine AS bob-the-builder
LABEL authors="kalyanmudumby"
WORKDIR /build

# Dependency layer: only invalidated when the POM changes.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline

COPY src ./src
ARG ENV
# `package` produces the executable jar; `install` additionally wrote it to the local repository,
# which the image never reads. Tests run in CI, not here.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package -P"${ENV:-default}"

# Split the jar so dependencies, which change rarely, land in a different image layer to the
# application classes, which change on every commit. The extracted jar keeps its versioned name;
# rename it so the entrypoint does not have to glob.
RUN java -Djarmode=tools -jar target/documan*.jar extract --layers --destination extracted \
 && mv extracted/application/documan*.jar extracted/application/application.jar

# ---------------------------------------------------------------- agent
FROM eclipse-temurin:25-alpine AS agent
ARG OTEL_AGENT_VERSION=2.9.0
ARG OTEL_AGENT_SHA256=fa039f86082b559e53283a863e03de5df7db4bdb4840c5cbb8bf38f292987d5b
# Verified rather than trusted: the previous build pulled this jar on every image build with no
# integrity check at all.
RUN wget -q -O /otel.jar \
      "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar" \
 && echo "${OTEL_AGENT_SHA256}  /otel.jar" | sha256sum -c -

# ---------------------------------------------------------------- runtime
FROM eclipse-temurin:25-alpine
WORKDIR /application

RUN addgroup -S documan && adduser -S -G documan -h /application documan

COPY --from=agent /otel.jar /application/otel.jar
COPY --from=bob-the-builder --chown=documan:documan /build/extracted/dependencies/ ./
COPY --from=bob-the-builder --chown=documan:documan /build/extracted/spring-boot-loader/ ./
COPY --from=bob-the-builder --chown=documan:documan /build/extracted/snapshot-dependencies/ ./
COPY --from=bob-the-builder --chown=documan:documan /build/extracted/application/ ./

# CONFIGURABLE
ENV SERVICE_NAME="OPTIMUS"
ENV CLIENT_NAME="EARTH"
ARG OTEL_ENDPOINT
ARG ENV="development"

ENV OTEL_EXPORTER_OTLP_ENDPOINT=$OTEL_ENDPOINT
ENV OTEL_RESOURCE_ATTRIBUTES="service.name=$SERVICE_NAME-${ENV},environment=${ENV},client=$CLIENT_NAME"
ENV OTEL_TRACES_SAMPLER="always_on"
ENV OTEL_INSTRUMENTATION_MICROMETER_ENABLED=true
ENV OTEL_INSTRUMENTATION_COMMON_DB_STATEMENT_SANITIZER_ENABLED=true
ENV OTEL_INSTRUMENTATION_LOGBACK_ENABLED=true
ENV OTEL_METRIC_EXPORT_INTERVAL=10000
ENV OTEL_METRICS_EXEMPLAR_FILTER=ALWAYS_ON

# MaxRAMPercentage rather than a fixed -Xmx so the heap tracks the container limit.
# AutoCreateSharedArchive writes a CDS archive on first start and reuses it afterwards, which cuts
# startup on any deployment that gives the container a writable volume at /application/cds.
ENV JAVA_OPTS="-javaagent:/application/otel.jar \
-XX:MaxRAMPercentage=75 \
-XX:+AutoCreateSharedArchive \
-XX:SharedArchiveFile=/application/cds/application.jsa"

RUN mkdir -p /application/cds && chown documan:documan /application/cds

USER documan
EXPOSE 8080

# sh -c so JAVA_OPTS is actually expanded. The previous entrypoint set JAVA_OPTS and then ignored
# it, hard-coding the arguments instead.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar application.jar \"$@\"", "--"]
