# Design Philosophy

While it is tempting to use co-routines, the fundamental interfaces are with _hardware_ devices -- and they don't necessarily like to be interrupted and paused. Thus, "standard" threading semantics should apply.

# LEDs and Resistors

There are more than a few resources available, but this is my "spin":

Using a generic red LED:

```
V -> 1.9 - 2.2 
i -> 10ma (rec) - 20ma (max)
```

where V<sub>d</sub> == 3.3v - V<sub>led</sub>

| i vs V<sub>d</sub> | 1.1 | 1.4 |
|--------------------|-----|-----|
| 20ma               | 65  | 70  |
| 16ma               | 69  | 88  |
| 10ma               | 110 | 140 |

Most kits come with 220 ohm resistors for the LED project:

```
1,4V / 220 ohm = 6ma
and
1.1V / 220 ohm = 4ma
```
