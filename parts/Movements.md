# Moving Parts

Classes to describe interacting in a physical world.

- A `Movement` describes some physical change that is desired
  - A "stop check" function can be supplied to also execute code as a limiter (or call-back)
  - A special case is provided for "just run some code" without requiring a physical component
- An `Actuator<Movement>`is the controller for a thing that does a type of `Movement`
  - `Rotator` for moving things in arcs (`RotationMovement`)
  - `LinearActuator` for pushme-pullyou operations (`LinearMovement`)
- An `Action` is a set of `Actuator`/`Movement` pairs to be performed as a single unit, until all `Movement`s are complete
  - note that only one `Movement` per `Actuator` is allowed: multiple definitions will over-write one another
  - the exception are `execution` blocks, which are treated independently
- An `ActionSequence` is a basic list of `Actions` to be done sequentially

The sequences and actions are contained within a type-safe DSL. Actions and movements are **rebuilt** when retrieved from a sequence: this allows altering external variables that would influence _subseqeuent_ re-invocations of that same sequence.

## :bangbang: IMPORTANT!!! Recommendations for Execution

Currently, no "sequence executors" are defined, but there are _some_ quirks.

- the `stopCheck` of a `Movement` should be checked _prior_ to executing the `move` requested
- once a `Movement` is "complete" (`move` returns true or the `stopCheck` fires), that movement should **not** be re-invoked

## Actuators and Movements

### Rotation

Fairly self-descriptive, a `Rotator` has a "turning" component. All instances of this type have a `RotationMovement`defined in terms of angles of degrees.<sup>**1**</sup>

A gear-ratio determines how much an angular change of the motor translates to the physical change in the system. Motors _may_ also have restrictions on their spindle movements (e.g. servos can only move 180 degrees).

#### Basic `BasicStepperRotator`

This `Rotator` uses stepper motors to move to a position by keeping track of "steps from zero". Note only that motors that will "single-step" can be used here.

- the number of steps per rotation is translated to _steps per degree_
- positioning is _not_ very precise due to rounding errors
- "zero" position is entirely arbitrary and is assumed to be pre-calibrated
- there are no angular limits imposed, so over-rotation is a possibility

#### Simple `ServoRotator`

Models the attachment to a non-continuous servo. The physical movement is "mapped" to the servo's angular displacement.

### Linear

A `LinearMovement` causes a `LinearActuator` to move `x` **percentage** of the physical movement: this is mapped to `0->100` (fully retracted to fully extended),

Only a single actuator is currently defined:

- `ServoLinearActuator` which maps the `0->100` range to servo angles (e.g. gear-driven slider or piston)

### Special

- _ExecutionMovement_ is a "pseudo actuator and movement" that does **not** require a physical component, only executes the given code block as the _stop check_.

## DSL By Example

```kotlin

lateinit var rotatorOne:Rotator
lateinit var rotatorTwo:Rotator
lateinit var pusher:LinearActuator

val exampleSequence  = sequence {
    var execDone = false
  
    // defines a set of movement to be performed as a unit
    // the action is finished when all actuators are "complete"
    action {
        // move rotatorOne to 90 degrees or until the check returns `true`
        rotatorOne rotate {
            angle = 90
            stopCheck = { someExternalBooleanCheck() }
        }
        // move 5 degrees
        rotatorTwo rotate 5
    
        // linear works the same way = 50% "extended"
        pusher extend {
            distance = 50
            stopCheck = { anotherCheck() }
        }
    }
  
    // another action - executed after the previous
    action {
        // rotates until the stop condition or the rotator's "limits" are reached
        rotatorOne backwardUntil { execDone }
        rotatorTwo forwardUntil { 
            val foo = bar()
            foo > 3000
        }
        pusher goTo 66
        // RUN CODE!
        execution {
            if (!execDone) execDone = someProcess()
            execDone
        }
    }
  
    // add an externally defined action (builder)
    this + externallyDefinedAction
  
    // concatenate sequences
    this += concatenatedSequences
}
```

# TODO

<sup>**1**</sup>Need to accommodate continuous rotation servos and "chopper" steppers - e.g. conditions where "angle" won't work.
