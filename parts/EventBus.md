# Simple Event Bus

A _very_ simple, quasi-type driven pub-sub event bus. This uses the built-in `Publisher`/`Subscriber` classes from the basic Java implementations.

Currently, this uses the default implementations in terms of threads and buffers. Each "topic" is created with an individual `Producer`.

## Classes

![](EventBus.png)

## Usage

THe first thing to note is that there is no real type safety availble: each subscriber must be able to handle the messages put on the subscribed topic, so some cooperation between the producer and all subscribers are necessary.

`KobotsEvent` and `KobotsAction` are intended to provide separation between _requesting_ "actions" and "events" _occurring_ in the system. Examples:

- the [`SequenceRequest`](src/main/kotlin/crackers/kobots/execution/Messages.kt) is a `KobotsAction` for requesting physical changes (movement).
- a sensor polling in a separate thread can send out measurement or alert messages via `KobotsEvent` implementations

Use either of the `publitshToTopic` methods to send one or more requests or events.

Each `KobotSubscriber` is wrapped in an appropriate Java `Subscriber` and will receive as many messages up to the number specified. The default is one at a time. 
