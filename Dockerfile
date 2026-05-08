 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.8

FROM hmctsprod.azurecr.io/base/java:21-distroless

#DEBUG - Remove everything between here
WORKDIR /opt/app

USER hmcts

##### And here in final image

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/app-register.jar /opt/app/

WORKDIR /opt/app

EXPOSE 4550
ENTRYPOINT ["java","-Duser.timezone=UTC","-jar","/opt/app/app-register.jar"]
