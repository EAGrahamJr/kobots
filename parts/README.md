# Parts is Parts

Abstractions to orchestrate physical device interactions. The actual _execution_ of the interactions is not defined here (this avoids the "threads/no-threads" issue temporarily :grinning: )

- `Movement` - describes some physical change
- `Actuator<Movement>` - a thing that does a type of `Movement`
  - `Rotator` are `Actuator`s for `RotationMovement`
  - There are `LinearActuator` as well for pushme-pullyou operations
- `Action`s define a series of `Movement`s to be performed "at the same time"
- `ActionSequence` defines, well, a sequence of `Action`s
  - Example: "go to location One and pick up thingie and move it to location Two"
- The sequences and actions are contained within a type-safe DSL
- Actions and movements are **rebuilt** on every invocation - this allows for repeated items to influence subsequent iterations
  - That's _programming_, y'all 
