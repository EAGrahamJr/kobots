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

from time import sleep

from Adafruit_LCD2004 import Adafruit_CharLCD
from PCF8574 import PCF8574_GPIO


def start_test(testname):
    lcd.clear()
    lcd.home()  # set cursor position
    lcd.message('EdG Test')

    lcd.setCursor(0, 1)
    lcd.message(testname)
    lcd.home()
    print(testname)
    sleep(.5)


def the_test():
    chars = '01234567890ABCDEF7890'[:lcd.numcols]

    start_test('Shift right')
    lcd.message(chars)
    sleep(1)
    lcd.scrollDisplayRight()
    sleep(2)
    lcd.setCursor(1, 1)
    lcd.message('Shift left')
    sleep(1)
    lcd.scrollDisplayLeft()
    sleep(2)

    start_test('Autoscroll')
    lcd.autoscroll()
    for c in chars:
        lcd.message(c)
        sleep(.2)
    lcd.noAutoscroll()
    sleep(1)

    start_test('Cursor visible')
    lcd.message('stuff ')
    lcd.cursor()
    sleep(.5)
    lcd.blink()
    sleep(2)
    lcd.noBlink()
    sleep(.5)
    lcd.noCursor()
    sleep(1)

    # TODO cursor movement not in original code

    # TODO backlght toggle?
    start_test('Display toggle')
    for i in range(1, 4):
        lcd.noDisplay()
        sleep(1)
        lcd.display()

    # TODO custom character not in original code

    if lcd.numlines > 2:
        start_test('Extra lines!!!')
        lcd.setCursor(0, 2)
        lcd.message('Extra lines')
        lcd.setCursor(4, 3)
        lcd.message('go here...')
        lcd.cursor()
        lcd.blink()
        sleep(2)

    start_test('Completed')
    sleep(5)


def destroy():
    lcd.clear()
    mcp.output(3, 0)


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

    mcp.output(3, 1)  # turn on LCD backlight
    lcd.begin(20, 4)  # set number of LCD lines and columns

    the_test()

    destroy()
