FROM balenalib/raspberry-pi-openjdk:8-stretch

COPY target/docker/stage/opt /opt
WORKDIR /opt/docker

RUN apt-get update && apt-get install wiringpi
#RUN apt-get update && apt-get install -y --no-install-recommends apt-utils
#RUN apt-get install wiringpi
ADD --chown=daemon:daemon opt /opt
USER daemon

ENTRYPOINT ["/opt/docker/bin/main"]
CMD []