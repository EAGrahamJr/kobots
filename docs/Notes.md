# Design Philosophy

Re-invent the wheel. Well, not the wheel, but pretty much everything else: no kits.

- Why not [ROS](https://www.ros.org/)?
  - _Foremost_, it's C/Python and not what I want to play with
  - This is a _learning_ exercise: not a lot of fun in a "solved" problem (although cribbing is likely to happen)
  - The basic design **concepts** are going to be the same, so similarities will result

## Kotlin Co-routines

While it is tempting to use co-routines, the fundamental interfaces are with _hardware_ devices -- and they don't necessarily like to be interrupted and paused.

~~The "event bus" concept for communicating between items **can** use co-routines, as long as anything interacting with hardware is wrapped in the appropriate blocking context.~~

Using the Java `Publisher/Subscriber` (which uses the `ForkJoinPool`) seems to work just fine, as there is apparently decent blocking on the I2C bus. GPIO might need more care, and SPI and CAN are still and unknown quantities.

## 5-Wire Stepper to JST plug colors

| Stepper | JST    | Pin |
|---------|--------|-----|
| blue    | black  | 4   |
| pink    | red    | 3   |
| yellow  | white  | 2   |
| orange  | yellow | 1   |
| red     | orange | 5v  |
