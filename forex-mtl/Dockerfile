FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.1_3.2.0

WORKDIR /app

COPY build.sbt .
COPY project/ project/

RUN sbt update

COPY src/ src/

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=5 \
  CMD curl -fsS http://localhost:8080/health || exit 1

ENV CONFIG_FILE=application-docker.conf

CMD ["sbt","run"]



