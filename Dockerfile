FROM openjdk:latest
WORKDIR /
COPY "akka-quizz.jar" "/app.jar"
CMD ["java", "-jar", "/app.jar"]