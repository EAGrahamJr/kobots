#!/usr/bin/env python3

#  Copyright 2022-2023 by E. A. Graham, Jr.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
#  or implied. See the License for the specific language governing
#  permissions and limitations under the License.

from datetime import datetime
from time import sleep

from Adafruit_LCD2004 import Adafruit_CharLCD
########################################################################
# Filename    : I2CLCD2004.py
# Description : Use the LCD display data
# Author      : freenove
# modification: 2022/06/28
########################################################################
from PCF8574 import PCF8574_GPIO


def get_cpu_temp():  # get CPU temperature and store it into file "/sys/class/thermal/thermal_zone0/temp"
    tmp = open('/sys/class/thermal/thermal_zone0/temp')
    cpu = tmp.read()
    tmp.close()
    return '{:.2f}'.format(float(cpu) / 1000) + ' C'


def get_time_now():  # get system time
    return datetime.now().strftime('    %H:%M:%S')


def loop():
    mcp.output(3, 1)  # turn on LCD backlight
    lcd.begin(20, 4)  # set number of LCD lines and columns
    lcd.setCursor(0, 0)  # set cursor position
    lcd.message('FREENOVE')
    lcd.setCursor(0, 1)  # set cursor position
    lcd.message('wwww.freenove.com')
    while (True):
        # lcd.clear()
        lcd.setCursor(0, 2)  # set cursor position
        lcd.message('CPU: ' + get_cpu_temp() + '\n')  # display CPU temperature
        lcd.setCursor(0, 3)  # set cursor position
        lcd.message(get_time_now())  # display the time
        sleep(1)


def destroy():
    lcd.clear()


PCF8574_address = 0x27  # I2C address of the PCF8574 chip.
PCF8574A_address = 0x3F  # I2C address of the PCF8574A chip.
# Create PCF8574 GPIO adapter.
try:
    mcp = PCF8574_GPIO(PCF8574_address)
except:
    try:
        mcp = PCF8574_GPIO(PCF8574A_address)
    except:
        print('I2C Address Error !')
        exit(1)
# Create LCD, passing in MCP GPIO adapter.
lcd = Adafruit_CharLCD(pin_rs=0, pin_e=2, pins_db=[4, 5, 6, 7], GPIO=mcp)

if __name__ == '__main__':
    print('Program is starting ... ')
    try:
        loop()
    except KeyboardInterrupt:
        destroy()
