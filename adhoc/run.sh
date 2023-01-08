#!/usr/bin/env sh
[ "$1" = "-s" ] && SUSPEND="y" || SUSPEND="n"
JAVA="/home/crackers/java"

$JAVA -ea -agentlib:jdwp=transport=dt_socket,server=y,suspend=$SUSPEND,address=*:5005 -jar marvin-pi.jar
