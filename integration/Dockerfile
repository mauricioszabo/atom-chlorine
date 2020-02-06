FROM jamesnetherton/docker-atom-editor

USER root

RUN apt-get update
RUN apt-get install -y wget gpg
RUN apt-get install -y xvfb ffmpeg netcat xz-utils openjdk-11-jdk

RUN cd /tmp && curl https://nodejs.org/dist/v10.16.3/node-v10.16.3-linux-x64.tar.xz | tar -xJ
RUN cp -a /tmp/node-v10.16.3-linux-x64/* /usr
RUN rm -R /tmp/node-v10.16.3-linux-x64

ENV DISPLAY=:80
ENV ELECTRON_ENABLE_LOGGING=true

RUN mkdir /root/.atom/packages -p
RUN echo "'*':\n  welcome:\n    showOnStartup: false\n  core:\n    telemetryConsent: \"no\"\n    disabledPackages: [\"github\"]\n  \"autocomplete-plus\":\n    enableAutoActivation: false" > /root/.atom/config.cson
RUN sh -c 'cd /root/.atom/packages && git clone https://github.com/JunoLab/atom-ink.git ink && cd ink && npm i'

WORKDIR /work
COPY . /work
RUN npm install
RUN npx shadow-cljs release dev --source-maps
RUN ln -s /work /root/.atom/packages/chlorine

CMD sh -c 'Xvfb -screen 0 1024x768x24+32 :80 & ./scripts/test'
