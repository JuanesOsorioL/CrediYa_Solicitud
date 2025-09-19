##FROM eclipse-temurin:21-jdk-alpine
#FROM gradle:8-jdk21 AS build
#WORKDIR /workspace
#COPY . .
#RUN ./gradlew :app-service:bootJar --no-daemon -x test
##COPY *.jar CrediYa_Solicitud.jar
#ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom"
#USER appuser
#EXPOSE 8080
#COPY --from=build /workspace/applications/app-service/build/libs/*.jar /app/app.jar
#ENTRYPOINT ["java","-jar","/app/app.jar"]
##ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS  -jar CrediYa_Solicitud.jar" ]




# -Xshareclasses:name=cacheapp,cacheDir=/cache,nonfatal -XX:+UseContainerSupport


#FROM gradle:8-jdk21 AS build
#ARG BOOT_MODULE="app-service"
#WORKDIR /workspace
#
#COPY . .
#RUN gradle --no-daemon clean :${BOOT_MODULE}:bootJar -x test
#
#FROM eclipse-temurin:21-jre
#ARG JAR_DIR="applications/app-service/build/libs"
#WORKDIR /app
#COPY --from=build /workspace/${JAR_DIR}/*.jar /app/app.jar
#EXPOSE 8080
#ENTRYPOINT ["java","-jar","/app/app.jar"]


FROM gradle:8-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon clean :app-service:bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/applications/app-service/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]