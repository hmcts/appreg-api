 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.7

FROM busybox:1.37.0-musl AS bb

FROM hmctsprod.azurecr.io/base/java:21-distroless


#DEBUG - Remove everything between here
WORKDIR /opt/app

COPY --from=bb /bin/busybox /busybox


USER root
RUN ["/busybox","mkdir","-p","/usr/bin"]
RUN ["/busybox","ln","-sf","/busybox","/usr/bin/ls"]
RUN ["/busybox","ln","-sf","/busybox","/usr/bin/cat"]
RUN ["/busybox","ln","-sf","/busybox","/usr/bin/sh"]
USER hmcts

##### And here in final image

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/app-register.jar /opt/app/

WORKDIR /opt/app

EXPOSE 4550
ENTRYPOINT ["java","-Duser.timezone=UTC","-jar","/opt/app/app-register.jar"]
