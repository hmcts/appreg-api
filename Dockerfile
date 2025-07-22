 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.1
FROM openjdk:21-jdk-slim-bullseye

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/app-register.jar /opt/app/

WORKDIR /opt/app

EXPOSE 4550
ENTRYPOINT ["java","-Duser.timezone=UTC","-jar","/opt/app/app-register.jar"]
