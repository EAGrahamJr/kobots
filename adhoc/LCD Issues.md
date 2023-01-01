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

Each test is performed after a full hardware power down and start.

- Python
- Java (Kotlin)

### Devices

- Python - *driver* code from [vendor](https://github.com/Freenove/Freenove_LCD_Module/tree/main/Freenove_LCD_Module_for_Raspberry_Pi/Python/Python_Code)
  - basically the same as the [OG Adafruit Python](https://github.com/adafruit/Adafruit_Python_CharLCD)
- HD - diozero HD44780Lcd
- Pi4J - from sample components
- Hack - Pi4J code running on LcdConnection
- EdG - my hack on everything

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
| autoscroll  |        |     |      |          |     |
| backlight   |        |     |      |          |     |
| cust. char  |        |     |      |          |     |

### 20x4

| Test        | Python               | HD  | Pi4J | LCD Hack | EdG |
|-------------|----------------------|-----|------|----------|-----|
| clear       | :+1:                 |     |      |          |     |
| home        | :+1:                 |     |      |          |     |
| write       | :+1:                 |     |      |          |     |
| position    | :+1:                 |     |      |          |     |
| move        | n/a                  |     |      |          |     | 
| visible     | :+1:                 |     |      |          |     |
| blink       | :+1:                 |     |      |          |     |
| shift left  | :+1:                 |     |      |          |     |
| shift right | :+1:                 |     |      |          |     |
| autoscroll  | :+1:                 |     |      |          |     |
| backlight   | :+1:<sup>**1**</sup> |     |      |          |     |
| cust. char  | n/a                  |     |      |          |     |

<sup>**1**</sup> enabled/disabled via direct GPIO write

---

# Appendix A: Adafruit Circuit Python

Some seriously updated code from Adafruit:

- [LCD](https://github.com/adafruit/Adafruit_CircuitPython_CharLCD/tree/main/adafruit_character_lcd)
- 
