# Denne containeren kjører med en non-root bruker
FROM ghcr.io/navikt/fp-baseimages/distroless:25

LABEL org.opencontainers.image.source=https://github.com/navikt/fp-inntektsmelding

COPY target/classes/logback*.xml conf/
COPY target/lib/*.jar lib/
COPY target/app.jar .
