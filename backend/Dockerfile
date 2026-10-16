# Compilacion en una imagen con JDK y ejecucion en una imagen solo con JRE,
# para que la imagen final no lleve Maven ni el codigo fuente.
FROM maven:3.9-eclipse-temurin-21 AS construccion
WORKDIR /origen
# Las dependencias se resuelven en una capa aparte y se reutilizan entre builds.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /aplicacion
# La aplicacion no corre como root.
RUN addgroup -S sgai && adduser -S sgai -G sgai
COPY --from=construccion /origen/target/*.jar aplicacion.jar
RUN chown -R sgai:sgai /aplicacion
USER sgai
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=America/Lima"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar aplicacion.jar"]
