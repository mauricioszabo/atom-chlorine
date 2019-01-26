FROM jamesnetherton/docker-atom-editor

USER root

RUN apt update
RUN apt install -y xvfb
#RUN apt install -y openjdk-8-jdk nodejs netcat
RUN apt install -y nodejs netcat

#RUN curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > /usr/bin/lein
#RUN chmod +x /usr/bin/lein

ENV DISPLAY=:99

RUN mkdir /root/.atom
RUN echo "'*':\n  welcome:\n    showOnStartup: false\n  core:\n    telemetryConsent: \"no\"\n    disabledPackages: [\"github\"]\n  \"autocomplete-plus\":\n    enableAutoActivation: false" > /root/.atom/config.cson

WORKDIR /work
CMD sh -c 'Xvfb :99 & ./scripts/test'
