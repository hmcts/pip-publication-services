FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-debug-1.0

ARG APP_INSIGHTS_AGENT_VERSION=2.5.1
ARG APP

# Application image
COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/gdsFont.otf /opt/app/
COPY build/libs/$APP /opt/app/

EXPOSE 8081
CMD [ "pip-publication-services.jar" ]
