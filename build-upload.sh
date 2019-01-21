#!/bin/bash

sbt assembly

scp target/scala-2.12/rasberry-pi-assembly-0.1.0-SNAPSHOT.jar pi@10.10.10.126:~
