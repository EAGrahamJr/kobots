# Extensions and Shared Junk

STILL UNDER CONSTRUCTION!!!! Check out the source directories for devices and utilities.

This will probably get broken up sometime in the future.

## A Note on Concurrency

Multithreaded operations are possible, and a framework is laid out in the [Flows](src/main/kotlin/crackers/kobots/ops/Flows.kt), there are certain inherent difficulties in doing so:

* Possible contention on the various hardware buses (e.g. I<sup>2</sup>C)
* Slave microcontrollers are typically underpowered in terms of raw processing power

If not carefully orchestrated, this can lead to unexpected device shutdowns durning application runtimes.
