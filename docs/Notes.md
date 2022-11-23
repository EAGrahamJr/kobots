# Design Philosophy

Re-invent the wheel. Well, not the wheel, but pretty much everything else: no kits.

## Kotlin Co-routines

While it is tempting to use co-routines, the fundamental interfaces are with _hardware_ devices -- and they don't necessarily like to be interrupted and paused. Thus, "standard" threading semantics should apply.
