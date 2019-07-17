#!/bin/sh

xhost +local:docker
docker run --mount type=bind,source=$(pwd)/collision_detection,target=/playground/collision_detection -v /tmp/.X11-unix:/tmp/.X11-unix -e DISPLAY=:0 -ti --rm hummingb1rd/opencv-contrib:latest
