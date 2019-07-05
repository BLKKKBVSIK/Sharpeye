#!/bin/sh

g++ -o predictor *.cpp `pkg-config opencv4 --cflags --libs` -std=c++17
