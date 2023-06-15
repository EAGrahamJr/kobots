# Demos

See the [picture gallery](https://photos.app.goo.gl/kWWJ8uUjWdHnsVDY6)

# Thoughts and Plans

- Actions (requests) produce states
- State transitions are "events"
  - are "events" propagated or does everyone just check current state?

## Servo/Stepper

Because both have the notion of _current position_: the servo can be queried and the stepper can be mainatined within the software, after "calibration" (set a "zero" mark).

### Actions

- Move to x position
- Start moving
  - Until stop
  - Until min/max
  - Bounce (change direction at max/min)
    - until stop
- Stop everything

### Attributes

- direction
  - compute from relative
- rate (angular velocity)
  - degrees/sec
    - but at what rate is it _updated_?
    - depends on "deadband" (5 microsec for SG90)

## DSL

- Perhaps using "string" to define which aspect of an "arm" (appendage?)
  - e.g. `"shoulder" at 44f stops {whatever}` 
