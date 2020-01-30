FROM jamesnetherton/docker-atom-editor

USER root

RUN apt-get update
RUN apt-get install -y wget gpg
RUN wget -qO - https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public | apt-key add -
RUN apt-get install -y software-properties-common
RUN add-apt-repository --yes https://adoptopenjdk.jfrog.io/adoptopenjdk/deb/
RUN apt-get install -y xvfb ffmpeg netcat xz-utils adoptopenjdk-11-openj9

RUN cd /tmp && curl https://nodejs.org/dist/v10.16.3/node-v10.16.3-linux-x64.tar.xz | tar -xJ
RUN cp -a /tmp/node-v10.16.3-linux-x64/* /usr
RUN rm -R /tmp/node-v10.16.3-linux-x64

ENV DISPLAY=:80
ENV ELECTRON_ENABLE_LOGGING=true

RUN mkdir /root/.atom
RUN echo "'*':\n  welcome:\n    showOnStartup: false\n  core:\n    telemetryConsent: \"no\"\n    disabledPackages: [\"github\"]\n  \"autocomplete-plus\":\n    enableAutoActivation: false" > /root/.atom/config.cson

WORKDIR /work


CMD sh -c 'Xvfb -screen 0 1024x768x24+32 :80 & ./scripts/test'
