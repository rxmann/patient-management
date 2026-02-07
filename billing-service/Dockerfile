# -----------------------
# Stage 1: Build the App
# -----------------------
# Use Maven image with Eclipse Temurin JDK 24 on Ubuntu Noble as the builder
FROM maven:3.9.9-eclipse-temurin-21-noble AS BUILDER

# Set the working directory inside the container
WORKDIR /app

# Copy only the pom.xml to leverage Docker layer caching for dependencies
COPY pom.xml .

# Download all Maven dependencies (offline build support)
RUN mvn dependency:go-offline -B

# Copy the full source code into the container
COPY src/ ./src

# Build the application (compiles and creates the JAR)
RUN mvn clean package

# --------------------------
# Stage 2: Run the Built App
# --------------------------
# Use a lightweight OpenJDK 24 image to run the built application
FROM openjdk:21-jdk AS RUNNER

# Set the working directory for the runtime container
WORKDIR /app

# Copy the packaged JAR file from the build stage
COPY --from=BUILDER ./app/target/billing-service-0.0.1-SNAPSHOT.jar ./app.jar

# Expose ports 4001 and 9001
EXPOSE 4001
EXPOSE 7001

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]
