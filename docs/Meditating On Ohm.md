# Ohm's Law and Components

This is where previous excursions into "electronics" always stopped me, so I made notes. These are based on the components and materials from the [Freenove Ultimate Starter Kit](https://github.com/Freenove/Freenove_Ultimate_Starter_Kit_for_Raspberry_Pi).

## LEDs and Resistors

There are more than a few resources available, but I could not figure out why the tutorial I was using had a certain value. Thus, this is my "spin":

Using a generic red LED:

```
V -> 1.9 - 2.2 
i -> 10ma (rec) - 20ma (max)
```

where V<sub>d</sub> == 3.3v - V<sub>led</sub>

| i vs V<sub>d</sub>   | 1.1 | 1.4 |
|----------------------|-----|-----|
| 20ma                 | 65  | 70  |
| 16ma<sup>**1**</sup> | 69  | 88  |
| 10ma                 | 110 | 140 |

<sup>**1**</sup> Default supplied by Raspberry Pi.

Common resistor values in the range are:

| R (ohm)             | 1.1  | 1.4  |
|---------------------|------|------|
| 68                  | 16ma | 20ma |
| 100                 | 11ma | 14ma |
| 150                 | 7ma  | 9ma  |
| 220<sup>**2**</sup> | 4ma  | 6ma  |

<sup>**2**</sup> Commonly supplied in electronics kits.

The answer to my question was that 220 ohms was "good enough" for most of the lessons and would light up the LED.

## Photoresistor

From the Freenove Lesson 10, there were no values for this component were given at **all**, so working _backwards_ from the circuit diagram:

i = V<sub>in</sub> / (R<sub>1</sub> + R<sub>2</sub>)<br/>
V<sub>out</sub> = i * R<sub>2</sub><br/>
V<sub>out</sub> = V<sub>in</sub> * R<sub>2</sub> / (R<sub>1</sub> + R<sub>2</sub>)<br/>

Given the circuit where the photoresistor is in position R<sub>2</sub>, observed max V<sub>out</sub> is about 2.25v, so the photoresistor has a range of approximately 0 to 22k ohm.
