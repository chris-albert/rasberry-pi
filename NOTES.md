

To publish zio-raspberry-ws281x to docker hub
```
sbt 'project zio-raspberry-ws281x' 'release with-defaults'
```

Running server locally
```
sbt 'server/run' 

```

To get to publish to dockerhub you need to login to docker from the command line 
```
docker login -u "username" -p "password"
```

To run on raspberry pi
```
docker run --user=root --privileged chrisalbert/rasberry-pi:0.0.16
```

To see GPIO table:
```
gpio readall
``` 

SSH into raspberry pi
```
ssh pi@192.168.42.37
```
