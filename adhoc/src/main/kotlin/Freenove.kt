import crackers.kobots.utilities.elapsed
import java.time.Instant
import kotlin.system.exitProcess

/**
 * Tutorials from Freenove "ultimate" Raspberry Pi starter set at
 * [Github](https://github.com/Freenove/Freenove_Ultimate_Starter_Kit_for_Raspberry_Pi)
 */
fun main() {
    println("Starting the tutorial main class")
    try {
//        `pretty print pins`()
//        `lesson 2`()
//        `lesson 2 with my stuff`()
//        `lesson 2 - debounce`()
//        `lesson 4`()
//        `lesson 5`()
//        `lesson 5 with my stuff`()
//        `rainbow because`()
//        `lesson 3`()
//        `lesson 3 with pwm evil grin`()
//        `lesson 6`()
//        `lesson 7`()
//        `lesson 9 but using my stuff`()
//        `lesson 11`()
//        `probe i2c`()
//        `lesson 13`()
        `lesson 14`()
    } catch (e: Throwable) {
        e.printStackTrace()
    }
    println("Ending tutorial code")
    exitProcess(0)
}

fun run5minutes(block: () -> Unit) {
    val start = Instant.now()
    while (start.elapsed().seconds < 300) {
        block()
    }
}
