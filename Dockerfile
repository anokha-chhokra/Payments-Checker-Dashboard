# --- Build stage: compile and package the WAR ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# --- Run stage: just the JRE + the packaged app ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.war app.war

# H2 file database lives here; mount a volume on this path to persist data
# across container restarts (see README for the docker run command).
VOLUME /app/data

EXPOSE 7060
ENTRYPOINT ["java", "-jar", "app.war"]
