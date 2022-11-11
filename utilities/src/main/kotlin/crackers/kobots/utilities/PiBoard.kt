package crackers.kobots.utilities

import com.diozero.api.DebouncedDigitalInputDevice
import com.diozero.api.GpioPullUpDown
import com.diozero.api.PinInfo
import com.diozero.api.PwmOutputDevice
import com.diozero.internal.spi.GpioDeviceFactoryInterface
import com.diozero.sbc.BoardPinInfo
import com.diozero.sbc.DeviceFactoryHelper
import sun.jvm.hotspot.oops.CellTypeState.value
import java.time.Duration
import java.util.function.LongConsumer

/**
 * Probably specific to the Raspbarry Pi.
 */
const val PI_HEADER = "J8"

/**
 * Default PWM freq
 */
const val DEFAULT_PWM_FREQUENCY: Int = 50

/**
 * Locate `PinInfo` for a selected board by name and header (defaults to Pi).
 */
fun BoardPinInfo.byName(name: String, header: String = PI_HEADER) =
    headers[header]?.values?.first {
        it.name == name
    } ?: throw NoSuchElementException("Header '$name' not found")

val nativeDeviceFactory by lazy { DeviceFactoryHelper.getNativeDeviceFactory() }
val defaultPins: BoardPinInfo by lazy { nativeDeviceFactory.getBoardPinInfo() }
val piPins: Collection<PinInfo>? by lazy { defaultPins.headers[PI_HEADER]?.values }

/**
 * Pretty-print the pin names, device number, and physical pin.
 */
fun `pretty print pins`() {
    val columnWidth = piPins!!.maxOf { it.name.length }
    val template = "| %-${columnWidth}s | %3s | %3d |"
    println("| %${columnWidth}s | Num | Pin |".format("Name"))
    println("|${"-".repeat(columnWidth + 2)}|-----|-----|")
    piPins!!.forEach {
        println(template.format(it.name, if (it.deviceNumber == -1) "" else "%3d".format(it.deviceNumber), it.physicalPin))
    }
}

/**
 * Simple extension to get the `whenPressed` and `whenReleased` semantics with debounce.
 */
class DebouncedButton @JvmOverloads constructor(
    gpio: Int,
    debounceTime: Duration,
    pud: GpioPullUpDown = GpioPullUpDown.NONE,
    activeHigh: Boolean = pud != GpioPullUpDown.PULL_UP,
    deviceFactory: GpioDeviceFactoryInterface = nativeDeviceFactory
) : DebouncedDigitalInputDevice(deviceFactory, deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(gpio), pud, activeHigh, debounceTime.toMillis().toInt()) {

    /**
     * Action to perform when the button is pressed.
     * @param buttonConsumer Callback function to invoke when pressed (long parameter is nanoseconds time).
     */
    fun whenPressed(buttonConsumer: LongConsumer) {
        whenActivated(buttonConsumer)
    }

    /**
     * Action to perform when the button is released.
     * @param buttonConsumer Callback function to invoke when released (long parameter is nanoseconds time).
     */
    fun whenReleased(buttonConsumer: LongConsumer) {
        whenDeactivated(buttonConsumer)
    }
}
