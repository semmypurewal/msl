#!/bin/sh

mkdir -p build
javac -d build -cp lib/msl-0.1.0-SNAPSHOT.jar:lib/bcprov-jdk15on-150.jar:lib/servlet-api-2.5.jar `find . -name "*.java"`
