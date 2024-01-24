FROM openjdk:latest
WORKDIR /
COPY "akka-quizz-assembly-0.1.0-SNAPSHOT.jar" "/app.jar"
CMD ["java", "-jar", "/app.jar"]