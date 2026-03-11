# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# DART API(opendart.fss.or.kr)가 TLS_RSA_WITH_AES_128_GCM_SHA256만 지원하는데,
# Java 17은 보안상 TLS_RSA_* cipher를 기본 비활성화하므로 이를 허용한다.
RUN sed -i '/^jdk.tls.disabledAlgorithms=/,/^$/{ s/TLS_RSA_\*, //g; s/ECDH, //g; }' \
    /opt/java/openjdk/conf/security/java.security

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
