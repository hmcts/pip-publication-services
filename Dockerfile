ARG APP_INSIGHTS_AGENT_VERSION=3.4.14
FROM hmctspublic.azurecr.io/base/java:21-distroless

ENV APP pip-publication-services.jar

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/$APP /opt/app/

USER 65532:65532

EXPOSE 8081
CMD [ "pip-publication-services.jar" ]
