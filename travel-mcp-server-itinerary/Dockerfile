# Multi-stage Dockerfile for Travel MCP Server Itinerary
# This Dockerfile builds the entire project and extracts the specific module's JAR

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy parent pom.xml and all module pom.xml files first for dependency caching
COPY pom.xml ./

# Copy all module directories
COPY travel-common ./travel-common
COPY travel-mcp-protocol ./travel-mcp-protocol
COPY travel-mcp-client ./travel-mcp-client
COPY travel-module-itinerary-biz ./travel-module-itinerary-biz
COPY travel-mcp-server-itinerary ./travel-mcp-server-itinerary

# Build the itinerary server module with dependencies
WORKDIR /app/travel-mcp-server-itinerary
RUN mvn dependency:go-offline -B || true
RUN mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the JAR file from builder stage
COPY --from=builder /app/travel-mcp-server-itinerary/target/*.jar app.jar

# Expose port
EXPOSE 8085

# Set environment
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
