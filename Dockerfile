 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-JavaFROM debian:latest AS build-stage
RUN apt-get update && apt-get install -y bash

ARG APP_INSIGHTS_AGENT_VERSION=3.7.1
FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY --from=build-stage /bin/bash /bin/bash
COPY lib/applicationinsights.json /opt/app/
COPY build/libs/app-register.jar /opt/app/

RUN apt-get update && \
    apt-get install -y bash

EXPOSE 4550
ENTRYPOINT ["java","-Duser.timezone=UTC","-jar","/opt/app/app-register.jar"]
