FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sh ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN sh ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build --chown=app:app /build/target/trung-cang-his-0.0.1-SNAPSHOT.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Ho_Chi_Minh \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+ExitOnOutOfMemoryError -Duser.timezone=Asia/Ho_Chi_Minh"
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
