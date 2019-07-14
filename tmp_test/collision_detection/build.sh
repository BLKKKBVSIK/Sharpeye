#!/bin/sh

g++-8 -o predictor *.cpp `pkg-config opencv4 --cflags --libs` -std=c++17
