FROM openjdk:latest
WORKDIR /opt/docker
ADD --chown=daemon:daemon opt /opt
USER daemon
EXPOSE 9000
ENTRYPOINT ["/opt/docker/bin/akka-quizz"]
