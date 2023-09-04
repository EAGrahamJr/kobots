# Moving Parts

Classes to describe interacting in a physical world. The intent is to provide a means to _smoothly_ cause changes, avoiding large, immediate movements that can over-stress the physical systems, as well as allowing for bounds-checking and interrupts during execution.

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

## Actuators and Movements

Actuators have a small set of _operators_ that are supported:

- `+=` and `-+` for relative changes
- `+a` and `-a` (unary plus/minus) for increment and decrement
- `%` on a `LinearActuator` for positioning

Each `Actuator` type may also contain specific `infix` capable functions for more DSL-like behavior. Note that these functions are "direct drive" and should immediately affect the device.

### Rotation

Fairly self-descriptive, a `Rotator` has a "turning" component. All instances of this type have a `RotationMovement`defined in terms of angles of degrees.<sup>**1**</sup>

A gear-ratio determines how much an angular change of the motor translates to the physical change in the system. Motors _may_ also have restrictions on their spindle movements (e.g. servos can only move 180 degrees).

#### BasicStepperRotator

This `Rotator` uses stepper motors to move to a position by keeping track of "steps from zero". Note only that motors that will "single-step" can be used here.

- the number of steps per rotation is translated to _steps per degree_
- **positioning is _not_ very precise due to rounding errors**
- "zero" position is entirely arbitrary and is assumed to be pre-calibrated
- there are no angular limits imposed, so over-rotation is a possibility

#### ServoRotator

Models the attachment to a **non-continuous** servo. The physical movement is "mapped" to the servo's angular displacement.

- due to rounding errors, some _physical_ angles may cause servo issues (e.g. jitter)
- some angles may not be reachable (e.g. if the gear ratios work out to a servo being set to a _partial_ angle, it may not actually move)

### Linear

A `LinearMovement` causes a `LinearActuator` to move `x` **percentage** of the physical movement: this is mapped to `0->100` (fully retracted to fully extended),

Only a single actuator is currently defined:

- `ServoLinearActuator` which maps the `0->100` range to servo angles (e.g. gear-driven slider or piston)

### Special

- _ExecutionMovement_ is a "pseudo actuator and movement" that does **not** require a physical component, only executes the given code block as the _stop check_.

## Sequence Executor

The [SequenceExecutor](src/main/kotlin/crackers/kobots/parts/SequenceExecutor.kt) provides one means of executing sequences and actions. It is primarily intended to be used with the [Event Bus](EventBus.md).

This class provides a means to execute sequences in a _background thread_. The actions are executed sequentially, invoking the _stopCheck_ functions prior to moving each component, as well as providing a mans of signalling an _immediate_ stop. The actions are executed in a way to provide pauses between invocations: this allows the physical systems to move incrementally, reducing stress (this speed can obviously be modified). The actual spped a step can execute will be limited by the hardware I/O.

Implementations of this class **MUST** handle any device or I/O contention -- this class does not provide any "locking" of devices.

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
