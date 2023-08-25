# Demos

See the [picture gallery](https://photos.app.goo.gl/kWWJ8uUjWdHnsVDY6)

# Thoughts and Plans

- [x] Actions (requests) produce states
- [x] State transitions are "events"
  - are "events" propagated or does everyone just check current state?

## Servo/Stepper

Because both have the notion of _current position_: the servo can be queried and the stepper can be mainatined within the software, after "calibration" (set a "zero" mark).

**Update** the stepper positioning by "counting" is not accurate and accumulates "drift" due to rounding errors.

### Actions

- [x] Move to x position
  - [ ]Start moving
  - [ ]Until stop
  - [x] Until min/max
  - [ ] Bounce (change direction at max/min)
    - [ ] set a range
- [x] Stop everything

### Attributes

- [x] direction
  - [x] compute from relative
- [ ] rate (angular velocity)
  - [x] simple delay
  - [ ] degrees/sec
    - but at what rate is it _updated_?
    - depends on "deadband" (5 microsec for SG90)

## DSL

~~Perhaps using "string" to define which aspect of an "arm" (appendage?) - e.g. `"shoulder" at 44f stops {whatever}~~
