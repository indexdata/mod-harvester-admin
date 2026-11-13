FROM folioci/eclipse-temurin:25-alpine

WORKDIR /usr/verticles
COPY target/mod-harvester-admin-fat.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=66.0"

# Expose this port locally in the container.
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/usr/verticles/app.jar"]
