# Multi-stage Dockerfile for Travel Web Application
# Independent API service (port 8080), separate from A2A Runtime

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy parent pom.xml and all module pom.xml files first for dependency caching
COPY pom.xml ./

# Copy all module directories
COPY travel-common ./travel-common
COPY travel-mcp-protocol ./travel-mcp-protocol
COPY travel-mcp-client ./travel-mcp-client
COPY travel-module-agent-biz ./travel-module-agent-biz
COPY travel-module-itinerary-biz ./travel-module-itinerary-biz
COPY travel-module-user-biz ./travel-module-user-biz
COPY travel-web ./travel-web

# Build the web module with dependencies
WORKDIR /app/travel-web
RUN mvn dependency:go-offline -B || true
RUN mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the JAR file from builder stage
COPY --from=builder /app/travel-web/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Set environment
ENV JAVA_OPTS="-Xms512m -Xmx1024m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
