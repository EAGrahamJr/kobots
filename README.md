# KOBOTS

Kotlin, Raspberry Pi, and Robots: what could possibly go worng.

Experiments in electronics, robotics, and interacting with the "real world" with functional languages<sup>**1**</sup>. The "base" implementations are projected to be based on the plethora of microcomputing platforms, specifically the Raspberry Pi.

This is a _learning_ experience for me, but it **might** produce something useful (I'll re-write this README if that actually happens :smiley:).

![Just Build](https://github.com/EAGrahamJr/kobots/actions/workflows/build.yaml/badge.svg)

## WHY????

A **long, long** time ago, I attended a [WorldCon](https://en.wikipedia.org/wiki/36th_World_Science_Fiction_Convention) and went to a panel on robotics. I was captivated. However, at the time, I was attending college -- it seemed that acid-etching PCBs in a dorm room was probably not going to be a thing.

... time passes ...

Today, it's stupid easy to "build" electronics on bread-boards, not to mention HATs, shims, extenders, etc., and there's stupidly powerful microprocessors that you can build with. Like running a JVM. That's a helluva step up from hand-held DTMF-based remote controls.

So, yeah - I have a hobby now.

## Libraries in Use

- [diozero](https://www.diozero.com/) - a thorough and straight-forward implementation of GPIO interface, with a **wide** range of standard devices to boot.<sup>**2**</sup>

:bangbang: Make sure the `i2c` controller is enabled on the Raspberry Pi and that your user is in the appropriate _group_ to access saiu devices:

```shell
$ ls -l /dev/i2c*
crw-rw---- 1 root i2c 89, 1 Nov 23 12:24 /dev/i2c-1
crw-rw---- 1 root i2c 89, 2 Nov 23 12:24 /dev/i2c-2
```

This [pinout reference](https://pinout.xyz/) is also invaluable...

# Building

This project uses [Gradle](https://gradle.org), so the only thing you need is a compatible JDK<sup>**3**</sup>. Additionally, because the project is [Kotlin](https://kotlinlang.org) and uses the _Kotlin Gradle plugin_, a Kotlin installation is also not necessary.

---

<sup>**1**</sup> All of the tutorials, lessons, kits, and existing libraries are predicated on C (at some level), due to the hardware interaction. I just don't want to go there anymore.<br/>
<sup>**2**</sup> [MIT License](https://github.com/mattjlewis/diozero/blob/main/LICENSE.txt)<br/>
<sup>**3**</sup> Java 17 is currently the only one used
