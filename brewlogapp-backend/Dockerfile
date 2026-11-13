# 1) Build
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# 2) Run
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 brewlog \
    && mkdir -p /app/uploads && chown brewlog /app/uploads
COPY --from=build /app/target/*.jar app.jar
USER brewlog
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]