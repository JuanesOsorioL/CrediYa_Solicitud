FROM gradle:8-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon clean :app-service:bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/applications/app-service/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]