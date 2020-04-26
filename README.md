# ZIO for rpi-ws281x-java

This is a ZIO layer around the [rpi-ws281x-java])(https://github.com/rpi-ws281x/rpi-ws281x-java)
library for controlling individual addressable LED strips. 

I dislike writing non-pure code now, so for this project I had to use ZIO, and the `Streams`
api makes is super simple to create animations. 

Inspiration from :
 - https://tutorials-raspberrypi.com/connect-control-raspberry-pi-ws2812-rgb-led-strips/
