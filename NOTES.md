

https://github.com/rpi-ws281x/rpi-ws281x-java


To publish to docker hub
```
sbt 'release with-defaults'
```

To get to publish to dockerhub you need to login to docker from the command line 
```j
docker login -u "username" -p "password"
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
