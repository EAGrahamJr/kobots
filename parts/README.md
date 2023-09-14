# Parts is Parts

Abstractions to orchestrate physical device interactions. The actual _execution_ of the interactions is not defined here (this avoids the "threads/no-threads" issue temporarily :grinning: )

There are two main sections.

- [Actuators and Movements](Movements.md)
- [Event Bus](EventBus.md)

## Other Stuff

- [`NeoKeyHandler`](src/main/kotlin/crackers/kobots/app/io/NeoKeyHandler.kt) is a "wrapper" around handling a rotating "menu" of actions
    - use with the `NeoKeyMenu` class to get complete key-press handling and automatic invocation of actions
- [`StatusColumnDisplay`](src/main/kotlin/crackers/kobots/app/io/StatusColumnDisplay.kt) displays numbers in named columns (e.g. for OLED and TFT displays)
