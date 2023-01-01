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

Each test-type is performed after a full hardware power down and start. Multiple runs per test-type are executed.

- Python
- Java (Kotlin)

### Devices

- Python - *driver* code from [vendor](https://github.com/Freenove/Freenove_LCD_Module/tree/main/Freenove_LCD_Module_for_Raspberry_Pi/Python/Python_Code)
  - basically the same as the [OG Adafruit Python](https://github.com/adafruit/Adafruit_Python_CharLCD)
- HD - diozero HD44780Lcd
- Pi4J - from [sample components](https://github.com/Pi4J/pi4j-example-components/blob/main/src/main/java/com/pi4j/catalog/components/LcdDisplay.java)
  - tried it just as a comparison point, without any expectations (just "out of the box")
- Hack - Pi4J code running on LcdConnection
- EdG - [my hack](https://github.com/EAGrahamJr/diozero/blob/lcd-interface/diozero-core/src/main/java/com/diozero/devices/SimpleLcd.java) on everything

### 20x4

| Test        | Python | HD<sup> | Pi4J | LCD Hack | EdG        |
|-------------|--------|---------|------|----------|------------|
| clear       | :+1:   | :-1:    | :-1: | :-1:     | :+1:       |
| home        | :+1:   | :-1:    | :-1: | :-1:     | :+1:       |
| write       | :+1:   | :-1:    | :-1: | :-1:     | :no_mouth: |
| position    | :+1:   | :-1:    | :-1: | :-1:     | :no_mouth: |
| move        | n/a    | :-1:    | :-1: | :-1:     | :no_mouth: | 
| visible     | :+1:   | :-1:    | :-1: | :-1:     | :no_mouth: |
| blink       | :+1:   | :-1:    | :-1: | :-1:     | :no_mouth: |
| shift left  | :+1:   | :-1:    | :-1: | :-1:     | :no_mouth: |
| shift right | :+1:   | :-1:    | :-1: | :-1:     | :no_mouth: |
| rt-to-lt    | :+1:   | n/a     | n/a  | n/a      | n/a        |
| autoscroll  | :+1:   | :-1:    | :-1: | :-1:     | :-1:       |
| backlight   | :+1:   | :-1:    | :-1: | :+1:     | :+1:       |
| cust. char  | n/a    | :-1:    | :-1: | :-1:     | :no_mouth: |

<sup>**1**</sup> enabled/disabled via direct GPIO write, as opposed via the device<br/>

#### Results

- The `HD44780Lcd` failed to function
  the cursor and display would occasionally turn on and would be in different locations, but no text was displayed
- Pi4J did not appear to write to the interface(s) at all (might be due to invalid setup?)
- Hack suggests that Pi4J may not be on the right page?
- EdG worked **briefly**, then proceeded to output garbage

**NOTE** running the _Python_ test immediately after any of the above returned the same results. This suggests that **none** of the aboe Java-based implemenations are actually correct. Also, since the difference between the code bases on the two sizes of displays is the same, this suggests that the same basic reslts will be had on that display as well.

### 16x2

| Test        | Python | HD  | Pi4J | LCD Hack | EdG |
|-------------|--------|-----|------|----------|-----|
| clear       |        |     |      |          |     |
| home        |        |     |      |          |     |
| write       |        |     |      |          |     |
| position    |        |     |      |          |     |
| move        |        |     |      |          |     | 
| visible     |        |     |      |          |     |
| blink       |        |     |      |          |     |
| shift left  |        |     |      |          |     |
| shift right |        |     |      |          |     |
| rt-to-lt    |        |     |      |          |     |
| autoscroll  |        |     |      |          |     |
| backlight   |        |     |      |          |     |
| cust. char  |        |     |      |          |     |

---

# Appendix A: Adafruit Circuit Python

Some seriously updated code from Adafruit:

- [LCD](https://github.com/adafruit/Adafruit_CircuitPython_CharLCD/tree/main/adafruit_character_lcd)
- 
