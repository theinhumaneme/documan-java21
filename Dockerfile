FROM maven:3-eclipse-temurin-21-alpine AS bob-the-builder
LABEL authors="kalyanmudumby"
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:resolve
RUN mvn dependency:resolve-plugins
COPY . .
ARG ENV
RUN if [ -z "$ENV" ]; then echo "no profile selected"; else echo "building ${ENV} profile"; fi
RUN if [ -z "$ENV" ]; then mvn install -Pdefault; else  mvn install -P$ENV; fi
RUN mv /build/target/documan-0.0.1-SNAPSHOT.jar /build/target/application.jar
FROM eclipse-temurin:21-alpine
COPY --from=bob-the-builder /build/target/application.jar application.jar
ENTRYPOINT ["java","-jar","application.jar"]
