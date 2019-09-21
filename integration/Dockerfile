FROM jamesnetherton/docker-atom-editor

USER root

RUN apt update
RUN apt install -y xvfb ffmpeg openjdk-8-jdk netcat xz-utils

RUN cd /tmp && curl https://nodejs.org/dist/v10.16.3/node-v10.16.3-linux-x64.tar.xz | tar -xJ
RUN cp -a /tmp/node-v10.16.3-linux-x64/* /usr
RUN rm -R /tmp/node-v10.16.3-linux-x64

ENV DISPLAY=:80
ENV ELECTRON_ENABLE_LOGGING=true

RUN mkdir /root/.atom
RUN echo "'*':\n  welcome:\n    showOnStartup: false\n  core:\n    telemetryConsent: \"no\"\n    disabledPackages: [\"github\"]\n  \"autocomplete-plus\":\n    enableAutoActivation: false" > /root/.atom/config.cson

WORKDIR /work
CMD sh -c 'Xvfb :80 & ./scripts/test'
