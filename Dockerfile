FROM maven:3-eclipse-temurin-21-alpine AS bob-the-builder
LABEL authors="kalyanmudumby"
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:resolve && mvn dependency:resolve-plugins
COPY . .
ARG ENV
RUN if [ -z "$ENV" ]; then mvn install -Pdefault; else mvn install -P"$ENV"; fi
FROM eclipse-temurin:21-alpine
COPY --from=bob-the-builder /build/target/documan*.jar application.jar
ENTRYPOINT ["java","-jar","application.jar"]
