#!/usr/bin/env sh
[ "$1" = "-s" ] && SUSPEND="y" || SUSPEND="n"
JAVA="/home/crackers/java"
JAR="/home/crackers/marvin-pi.jar"

sudo $JAVA -ea -agentlib:jdwp=transport=dt_socket,server=y,suspend=$SUSPEND,address=*:5005 -jar $JAR
