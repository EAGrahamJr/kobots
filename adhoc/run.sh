#!/usr/bin/env sh
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar marvin-pi.jar
