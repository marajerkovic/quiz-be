FROM openjdk:latest
WORKDIR /
COPY "akka-quizz_2.13-0.1.0-SNAPSHOT.jar" "/app.jar"
EXPOSE 10000
ENTRYPOINT ["/app.jar"]
