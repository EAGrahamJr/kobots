#!/usr/bin/env bash
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

# red led
# activity led
if [ "$1" = "on" ]; then
    sudo sh -c 'echo input > /sys/class/leds/PWR/trigger'
    sudo sh -c 'echo mmc0 > /sys/class/leds/ACT/trigger'
else
    sudo sh -c 'echo 0 > /sys/class/leds/PWR/brightness'
    sudo sh -c 'echo 0 > /sys/class/leds/ACT/brightness'
fi
