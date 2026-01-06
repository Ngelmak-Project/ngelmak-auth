FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /ngelmak

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package

FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /ngelmak

COPY --from=builder ./ngelmak/target/ngelmak-auth-0.0.1-SNAPSHOT.jar ./ngelmak-auth.jar

EXPOSE 4042

ENTRYPOINT ["java", "-jar", "ngelmak-auth.jar"]