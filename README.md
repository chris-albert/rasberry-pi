

https://github.com/rpi-ws281x/rpi-ws281x-java


To publish to docker hub
```
sbt 'release with-defaults'
```

To run on raspberry pi
```
docker run --user=root --privileged chrisalbert/rasberry-pi:0.0.9
```

To see GPIO table:
```
gpio readall
``` 


SSH into raspberry pi
```
ssh pi@192.168.42.37
```

password is 564136ca1394



FROM hypriot/rpi-java 
WORKDIR /opt/docker 
RUN apt-get update
RUN apt-get install wiringpi
ENTRYPOINT ["/opt/docker/bin/main"] 
CMD [] 

FROM hypriot/rpi-java
WORKDIR /opt/docker
ADD --chown=daemon:daemon opt /opt
USER daemon
ENTRYPOINT ["/opt/docker/bin/main"]
CMD []