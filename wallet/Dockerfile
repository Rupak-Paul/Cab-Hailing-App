FROM openjdk:8-jdk-alpine
ARG JAR_FILE=target/*.jar
ARG TXT_FILE=target/IDs.txt
COPY ${JAR_FILE} app.jar
COPY ${TXT_FILE} IDs.txt
ENTRYPOINT ["java","-jar","/app.jar"]
