# Extensions and Shared Junk

## Lazy Stuff

- [PiBoard](src/main/kotlin/crackers/kobots/utilities/PiBoard.kt) contains some lazy defaults

## Device Extensions

- [Common](src/main/kotlin/crackers/kobots/devices/Common.kt) contains some small wrappers:
  - `DebouncedButton` just wraps a debounced input with `Button` semantics (`whenPressed` and `whenReleased`)
- [AnodeRgbPwmLed](src/main/kotlin/crackers/kobots/devices/AnodeRgbPwmLed.kt) is for an RGB LED with common anode that is **inverted** from the _diozero_ `RgbPwmLed`. Provides the same interface.
