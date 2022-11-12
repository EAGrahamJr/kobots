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

- [diozero](https://www.diozero.com/) - a thorough and straigh-forward implementation of GPIO interface, with a **wide** range of standard devices to boot.

# Building

This project uses [Gradle](https://gradle.org), so the only thing you need is a compatible JDK<sup>**1**</sup>. Additionally, because the project is [Kotlin](https://kotlinlang.org) and uses the _Kotlin Gradle plugin_, a Kotlin installation is also not necessary.

<sup>**1**</sup> Java 17 is currently the only one used
