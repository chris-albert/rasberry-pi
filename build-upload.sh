#!/bin/bash

sbt assembly

scp target/scala-2.12/rasberry-pi-assembly-0.0.15-SNAPSHOT.jar pi@raspberrypi.local:~
