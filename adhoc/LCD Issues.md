# LCD Testing for diozero

- Both displays (16x2) and (20x4) are from the same distributor
- Both are using a PCF8574 backpack
  - 16x4 is "new style" without contrast adjustments and a single IC component
  - 20x4 has the older, larger "daughter board"
- testbed is a Raspberry Pi 3B+
  - Debian GNU/Linux 11 (bullseye)
  - Python 3.9.2
  - OpenJDK 64-Bit Server VM Temurin-17.0.5+8 (build 17.0.5+8, mixed mode, sharing)
  - i2c enabled, default clock speed
  - ```shell
    $ vcgencmd get_config arm_freq
    arm_freq=1200
    ```

## Test Matrix

1. Initially, each test-type was performed after a full hardware power down and start, with multiple runs per test-type executed.
2. Testing progressed to skipping the reboot, and simply re-powering the LCD in between test types.
3. Final stages simply swapped displays as needed, without power cycles between test types.

### Devices

- Python [Testnig](../adhoc/src/main/python/LCDTest.py)
  - *driver* code from [vendor](https://github.com/Freenove/Freenove_LCD_Module/tree/main/Freenove_LCD_Module_for_Raspberry_Pi/Python/Python_Code)
  - basically the same as the [OG Adafruit Python](https://github.com/adafruit/Adafruit_Python_CharLCD)
- HD - diozero HD44780Lcd
- `HackLcd` - [Pi4J-ish code](../adhoc/src/main/java/com/pi4j/HackLcd.java) running on diozero `LcdConnection`
  - based on a [sample component](https://github.com/Pi4J/pi4j-example-components/blob/main/src/main/java/com/pi4j/catalog/components/LcdDisplay.java)
  - also tried `Pi4J` directly, but was unable to get any functionality (didn't try too hard)
- SimpleLcd - [my hack](../adhoc/src/main/java/diozero/lcd/SimpleLcd.java) on everything

### 20x4

| Test        | Python             | HD         | HackLcd            | SimpleLcd          |
|-------------|--------------------|------------|--------------------|--------------------|
| clear       | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| home        | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| write       | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| position    | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| move        | :no_mouth:         | :x:        | :x:                | :heavy_check_mark: | 
| visible     | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| blink       | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| shift left  | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| shift right | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| rt-to-lt    | :heavy_check_mark: | :no_mouth: | :no_mouth:         | :no_mouth:         |
| autoscroll  | :heavy_check_mark: | :no_mouth: | :no_mouth:         | :no_mouth:         |
| backlight   | :heavy_check_mark: | :x:        | :heavy_check_mark: | :heavy_check_mark: |
| cust. char  | :no_mouth:         | :x:        | :x:                | :heavy_check_mark: |

### 16x2

| Test        | Python             | HD         | HackLcd            | SimpleLcd          |
|-------------|--------------------|------------|--------------------|--------------------|
| clear       | :heavy_check_mark: | :x:        | :heavy_check_mark: | :heavy_check_mark: |
| home        | :heavy_check_mark: | :x:        | :heavy_check_mark: | :heavy_check_mark: |
| write       | :heavy_check_mark: | :x:        | :heavy_check_mark: | :heavy_check_mark: |
| position    | :heavy_check_mark: | :x:        | :heavy_check_mark: | :heavy_check_mark: |
| move        | :no_mouth:         | :x:        | :heavy_check_mark: | :heavy_check_mark: | 
| visible     | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| blink       | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| shift left  | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| shift right | :heavy_check_mark: | :x:        | :x:                | :heavy_check_mark: |
| rt-to-lt    | :heavy_check_mark: | :no_mouth: | :no_mouth:         | :no_mouth:         |
| autoscroll  | :heavy_check_mark: | :no_mouth: | :no_mouth:         | :no_mouth:         |
| backlight   | :heavy_check_mark: | :x:        | :heavy_check_mark: | :heavy_check_mark: |
| cust. char  | :no_mouth:         | :x:        | :heavy_check_mark: | :heavy_check_mark: |

## Analysis

- `HD44780Lcd` failed to function
  - the cursor and display would occasionally turn on and would be in different locations, but no text was displayed
- `HackLcd`
  - on 16x2 shift test caused subsequent tests to fail oddly
- `Python`
  - writes a single **bit** at a time, but is _reading_ the data prior to writing and performing bit-ops on the existing data

**Possible Conclusion** The largest difference appaers to be around timing and which bits are where: note that the `SimpleLcd` class is a fusion of all the sources above, thus the success rate.

---

# Appendix A: Adafruit Circuit Python

Some seriously updated code from Adafruit:

- [LCD](https://github.com/adafruit/Adafruit_CircuitPython_CharLCD/tree/mai:no_mouth:dafruit_character_lcd)
- 
