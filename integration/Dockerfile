FROM jamesnetherton/docker-atom-editor

USER root

RUN apt update
RUN apt install -y xvfb
RUN apt install -y openjdk-8-jdk nodejs netcat

RUN curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > /usr/bin/lein
RUN chmod +x /usr/bin/lein

ENV DISPLAY=:0

WORKDIR /work
CMD sh -c 'Xvfb & ./scripts/test'
