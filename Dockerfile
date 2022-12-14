ARG APP_INSIGHTS_AGENT_VERSION=3.2.10
FROM hmctspublic.azurecr.io/base/java:17-distroless

ENV APP pip-publication-services.jar

COPY build/libs/$APP /opt/app/

EXPOSE 8081
CMD [ "pip-publication-services.jar" ]