# Design Philosophy

Re-invent the wheel. Well, not the wheel, but pretty much everything else: no kits.

- Why not [ROS](https://www.ros.org/)?
  - _Foremost_, it's C/Python and not what I want to play with
  - This is a _learning_ exercise: not a lot of fun in a "solved" problem (although cribbing is likely to happen)
  - The basic design **concepts** are going to be the same, so similarities will result

## Kotlin Co-routines

While it is tempting to use co-routines, the fundamental interfaces are with _hardware_ devices -- and they don't necessarily like to be interrupted and paused.

The "event bus" concept for communicating between items **can** use co-routines, as long as anything interacting with hardware is wrapped in the appropriate blocking context.
