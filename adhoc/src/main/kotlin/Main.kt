import crackers.kobots.utilities.piPins
import java.lang.Thread.sleep

fun main() {
    println("Name\tNumber\tPhysical")
    println("----\t------\t--------")
    piPins?.forEach {
        println("${it.name}\t${it.deviceNumber}\t${it.physicalPin}")
    }
    myStuff()
    sleep(60000)
}
