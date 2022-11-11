package crackers.kobots.utilities

import com.diozero.api.DebouncedDigitalInputDevice
import com.diozero.api.GpioPullUpDown
import com.diozero.api.PinInfo
import com.diozero.internal.spi.GpioDeviceFactoryInterface
import com.diozero.sbc.BoardPinInfo
import com.diozero.sbc.DeviceFactoryHelper
import java.time.Duration
import java.util.function.LongConsumer

/**
 * Probably specific to the Raspbarry Pi.
 */
const val PI_HEADER = "J8"

fun BoardPinInfo.byName(name: String, header: String = PI_HEADER) =
    headers[header]?.values?.first {
        it.name == name
    } ?: throw NoSuchElementException("Header '$name' not found")

val defaultPins: BoardPinInfo by lazy { DeviceFactoryHelper.getNativeDeviceFactory().getBoardPinInfo() }

val piPins: Collection<PinInfo>? by lazy { defaultPins.headers[PI_HEADER]?.values }

/**
 * Simple extension to get the `whenPressed` and `whenReleased` semantics with debounce.
 */
class DebouncedButton @JvmOverloads constructor(
    gpio: Int,
    debounceTime: Duration,
    pud: GpioPullUpDown = GpioPullUpDown.NONE,
    activeHigh: Boolean = pud != GpioPullUpDown.PULL_UP,
    deviceFactory: GpioDeviceFactoryInterface = DeviceFactoryHelper.getNativeDeviceFactory()
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
