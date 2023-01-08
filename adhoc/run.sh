#!/usr/bin/env zsh

[ "$1" = "-s" ] && SUSPEND="y" || SUSPEND="n"

OPTS="-ea -agentlib:jdwp=transport=dt_socket,server=y,suspend=$SUSPEND,address=*:5005"

# run with sudo for pigpio JNI library
nohup sudo $(which java) ${OPTS} -jar marvin-pi.jar  &!
