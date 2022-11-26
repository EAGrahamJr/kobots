# KOBOTS

Kotlin, Raspberry Pi, and Robots: what could possibly go worng.

Experiments in electronics, robotics, and interacting with the "real world" with functional languages (e.g. I hate C++ and dislike Python). The "base" implementations are projected to be based on the plethora of microcomputing platforms, specifically the Raspberry Pi.

This is a _learning_ experience for me, but it **might** produce sometning useful (I'll re-wrtie this README if that actually happens :smiley:).

## WHY????

A **long, long** time ago, I attended a [WorldCon](https://en.wikipedia.org/wiki/36th_World_Science_Fiction_Convention) and went to a panel on robotics. I was captivated. However, at the time, I was attending college and it seemed that acid-etching PCBs was probably not going to be a thing.

... time passes ...

Today, it's stupid easy to "build" electronics on bread-boards and there's stupidly powerful microprocessors that you can build with. Like running a JVM. That's a helluva step up from hand-held DTMF-based remote controls.

So, yeah - I have a hobby now.

## Libraries in Use

- [diozero](https://www.diozero.com/) - a thorough and straight-forward implementation of GPIO interface, with a **wide** range of standard devices to boot.

:bangbang: Make sure the `i2c` controller is enabled on the Raspberry Pi and that your user is in the appropriate _group_ to access saiu devices:

```shell
$ ls -l /dev/i2c*
crw-rw---- 1 root i2c 89, 1 Nov 23 12:24 /dev/i2c-1
crw-rw---- 1 root i2c 89, 2 Nov 23 12:24 /dev/i2c-2
```

This [pinout reference](https://pinout.xyz/) is also invaluable...

# Building

This project uses [Gradle](https://gradle.org), so the only thing you need is a compatible JDK<sup>**1**</sup>. Additionally, because the project is [Kotlin](https://kotlinlang.org) and uses the _Kotlin Gradle plugin_, a Kotlin installation is also not necessary.

<sup>**1**</sup> Java 17 is currently the only one used
