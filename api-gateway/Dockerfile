# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21-noble AS BUILDER
WORKDIR /app
COPY pom.xml .
# Only download dependencies
RUN mvn dependency:go-offline -B
COPY src ./src
# Skip tests to speed up image creation (assuming CI handles tests)
RUN mvn package -DskipTests

# Stage 2: Run
# Using 'jre' and 'jammy' (Ubuntu 22.04) for a smaller, stable footprint
FROM eclipse-temurin:21-jre-jammy AS runner
WORKDIR /app

# SECURITY: Create a non-privileged user
RUN useradd -ms /bin/bash rxman
USER rxman

# Copy the JAR from the builder
COPY --from=BUILDER /app/target/api-gateway-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 4004

# Using exec form for ENTRYPOINT is best practice
ENTRYPOINT ["java", "-jar", "app.jar"]
