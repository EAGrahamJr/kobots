#!/usr/bin/env sh
#
# Copyright 2022-2024 by E. A. Graham, Jr.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
# or implied. See the License for the specific language governing
# permissions and limitations under the License.
#

if [ "$1" = "-s" ]; then
  SUSPEND="y"
  shift
else
  SUSPEND="n"
fi

JAVA="/home/crackers/java"
JAR="/home/crackers/$1.jar"
LOG="/home/crackers/$1.log"
RUNTHIS="-jar $JAR"

ARGS="-ea"
# Uncomment to get debugging
ARGS="$ARGS -agentlib:jdwp=transport=dt_socket,server=y,suspend=$SUSPEND,address=*:5005"
# Uncomment to get remote monitoring
#ARGS="$ARGS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=8080"
#ARGS="$ARGS  -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
#ARGS="$ARGS -Djava.rmi.server.hostname=$(hostname -I | awk '{print $1}')"

# make sure no java process is running
killall -9 java 2>/dev/null
echo "Waiting 5 seconds for any previous Java app to exit..."
sleep 5
# starting the java app
echo "Starting Java app $JAR"
rm -f nohup.out
nohup $JAVA $ARGS $RUNTHIS > $LOG 2>&1 &
