#!/usr/bin/env sh
#
# Copyright 2022-2023 by E. A. Graham, Jr.
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

[ "$1" = "-s" ] && SUSPEND="y" || SUSPEND="n"
JAVA="/home/crackers/java"
JAR="/home/crackers/rotomatic-app.jar"

#if [ ! -z "$1" ]; then
#  CLASS=$(unzip -l $JAR | grep $1 | awk '{print $4}' | sed s,/,.,g | sed s/\.class//g | cut -d '$' -f 1)
#  echo "Running $CLASS"
#  RUNTHIS="-cp $JAR $CLASS"
#else
#  RUNTHIS="-jar $JAR"
#fi
RUNTHIS="-jar $JAR"

#echo $RUNTHIS
sudo $JAVA -ea -agentlib:jdwp=transport=dt_socket,server=y,suspend=$SUSPEND,address=*:5005 $RUNTHIS
