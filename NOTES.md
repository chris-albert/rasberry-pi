

To publish led-server to docker hub
```
sbt 'project server' 'release with-defaults'
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
docker run --user=root --privileged chrisalbert/led-server:0.0.19
```

To see GPIO table:
```
gpio readall
``` 

SSH into raspberry pi
```
ssh pi@192.168.42.37
```
