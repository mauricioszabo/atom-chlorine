FROM jamesnetherton/docker-atom-editor

USER root

RUN apt update
RUN apt install -y xvfb ffmpeg
#RUN apt install -y openjdk-8-jdk nodejs netcat
RUN apt install -y nodejs netcat

ENV DISPLAY=:80
ENV ELECTRON_ENABLE_LOGGING=true

RUN mkdir /root/.atom
RUN echo "'*':\n  welcome:\n    showOnStartup: false\n  core:\n    telemetryConsent: \"no\"\n    disabledPackages: [\"github\"]\n  \"autocomplete-plus\":\n    enableAutoActivation: false" > /root/.atom/config.cson

WORKDIR /work
CMD sh -c 'Xvfb :80 & ./scripts/test'
